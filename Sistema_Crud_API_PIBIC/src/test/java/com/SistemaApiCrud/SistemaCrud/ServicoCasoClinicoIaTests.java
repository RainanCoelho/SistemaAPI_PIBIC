package com.SistemaApiCrud.SistemaCrud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.SistemaApiCrud.SistemaCrud.dto.CasoClinicoAjusteRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.CasoClinicoGeradoIaDTO;
import com.SistemaApiCrud.SistemaCrud.dto.CasoClinicoIaRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.CasoClinicoIaResponseDTO;
import com.SistemaApiCrud.SistemaCrud.dto.PacienteGeradoIaDTO;
import com.SistemaApiCrud.SistemaCrud.entity.Professor;
import com.SistemaApiCrud.SistemaCrud.entity.CasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.ConteudoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.Paciente;
import com.SistemaApiCrud.SistemaCrud.entity.enums.EstadoCivil;
import com.SistemaApiCrud.SistemaCrud.entity.enums.NivelDificuldade;
import com.SistemaApiCrud.SistemaCrud.entity.enums.Sexo;
import com.SistemaApiCrud.SistemaCrud.entity.enums.StatusCasoClinico;
import com.SistemaApiCrud.SistemaCrud.exception.BusinessException;
import com.SistemaApiCrud.SistemaCrud.exception.CoerenciaCasoClinicoException;
import com.SistemaApiCrud.SistemaCrud.exception.ServicoIndisponivelException;
import com.SistemaApiCrud.SistemaCrud.repository.CasoClinicoRepository;
import com.SistemaApiCrud.SistemaCrud.repository.ConteudoClinicoRepository;
import com.SistemaApiCrud.SistemaCrud.repository.PacienteRepository;
import com.SistemaApiCrud.SistemaCrud.service.CasoClinicoAiClient;
import com.SistemaApiCrud.SistemaCrud.service.CasoClinicoIaTransactionService;
import com.SistemaApiCrud.SistemaCrud.service.GeracaoIaAuditService;
import com.SistemaApiCrud.SistemaCrud.service.ProtecaoDadosClinicosIa;
import com.SistemaApiCrud.SistemaCrud.service.ServicoCasoClinicoIa;

class ServicoCasoClinicoIaTests {

    private final CasoClinicoAiClient aiClient = mock(CasoClinicoAiClient.class);
    private final CasoClinicoRepository casoRepository = mock(CasoClinicoRepository.class);
    private final ConteudoClinicoRepository conteudoRepository = mock(ConteudoClinicoRepository.class);
    private final PacienteRepository pacienteRepository = mock(PacienteRepository.class);
    private final CasoClinicoIaTransactionService transactionService =
            mock(CasoClinicoIaTransactionService.class);
    private final GeracaoIaAuditService auditService = mock(GeracaoIaAuditService.class);

    @Test
    void deveExigirConfirmacaoDeDadosSinteticosOuDesidentificados() {
        CasoClinicoIaRequestDTO requisicao = new CasoClinicoIaRequestDTO();
        requisicao.setDadosSinteticosOuDesidentificados(false);

        assertThatThrownBy(() -> servicoComChave().gerarConteudo(1L, requisicao))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("sinteticos ou foram desidentificados");
    }

    @Test
    void deveCompletarCamposVaziosComRespostaDaIaEPreservarDadosDoProfessor() {
        ServicoCasoClinicoIa servico = servicoComChave();
        when(aiClient.gerarConteudo(any(), any()))
                .thenReturn(validacaoCoerencia("COERENTE"), respostaIa(), validacaoCoerencia("COERENTE"));

        CasoClinico caso = criarCaso();
        when(casoRepository.findById(1L)).thenReturn(Optional.of(caso));
        when(pacienteRepository.findByCasoClinicoIdCasoOrderByIdPacienteAsc(1L))
                .thenReturn(List.of());
        when(conteudoRepository.save(any(ConteudoClinico.class))).thenAnswer(invocation -> {
            ConteudoClinico conteudo = invocation.getArgument(0);
            conteudo.setIdConteudo(10L);
            return conteudo;
        });

        CasoClinicoIaRequestDTO requisicao = new CasoClinicoIaRequestDTO(
                "Febre relatada pelo professor",
                null,
                null,
                null,
                "Exacerbacao asmatica definida pelo professor");

        CasoClinicoIaResponseDTO resposta = servico.gerarConteudo(1L, requisicao);

        assertThat(resposta.getIdConteudo()).isEqualTo(10L);
        assertThat(resposta.getIdCaso()).isEqualTo(1L);
        assertThat(resposta.getSintomas()).isEqualTo("Febre relatada pelo professor");
        assertThat(resposta.getContexto()).isEqualTo("Paciente adulto em atendimento ambulatorial");
        assertThat(resposta.getExamClinico()).isEqualTo("Ausculta pulmonar com sibilos");
        assertThat(resposta.getAntecClinico()).isEqualTo("Historico de asma");
        assertThat(resposta.getDiagEsperado()).isEqualTo("Exacerbacao asmatica definida pelo professor");
    }

    @Test
    void deveRejeitarAntesDaGeracaoQuandoIncoerenciaForConfirmada() {
        ServicoCasoClinicoIa servico = servicoComChave();
        CasoClinicoGeradoIaDTO incoerente = validacaoCoerencia("INCOERENTE");
        incoerente.setViolacoes(Map.of(
                "especialidade", "A especialidade nao sustenta o diagnostico informado",
                "diagEsperado", "O diagnostico informado pertence a outro contexto clinico"));
        when(aiClient.gerarConteudo(any(), any()))
                .thenReturn(incoerente, incoerente);

        CasoClinico caso = criarCaso();
        caso.setEspecialidade("Atendimento ao Queimado");
        when(casoRepository.findById(1L)).thenReturn(Optional.of(caso));
        when(pacienteRepository.findByCasoClinicoIdCasoOrderByIdPacienteAsc(1L))
                .thenReturn(List.of());

        CasoClinicoIaRequestDTO requisicao = new CasoClinicoIaRequestDTO(
                null,
                null,
                null,
                null,
                "Possivel resfriado");

        assertThatThrownBy(() -> servico.gerarConteudo(1L, requisicao))
                .isInstanceOfSatisfying(CoerenciaCasoClinicoException.class, erro -> {
                    assertThat(erro.getCampos())
                            .containsKeys("especialidade", "diagEsperado");
                    assertThat(erro.getMessage()).contains("clinicamente incoerentes");
                });
        verify(transactionService, never()).executarGeracao(any(), any(), any(), any());
        verify(conteudoRepository, never()).save(any(ConteudoClinico.class));

        ArgumentCaptor<String> contextos = ArgumentCaptor.forClass(String.class);
        verify(aiClient, times(2)).gerarConteudo(any(), contextos.capture());
        assertThat(contextos.getAllValues().get(0))
                .contains(
                        "<dados_informados_pelo_usuario>",
                        "especialidade: Atendimento ao Queimado",
                        "diagEsperado: Possivel resfriado",
                        "violacoes");
        assertThat(contextos.getAllValues().get(1)).contains("<confirmacao>");
    }

    @Test
    void deveProsseguirQuandoSegundaAvaliacaoNaoConfirmaIncoerencia() {
        ServicoCasoClinicoIa servico = servicoComChave();
        CasoClinicoGeradoIaDTO primeiraAvaliacao = validacaoCoerencia("INCOERENTE");
        primeiraAvaliacao.setViolacoes(Map.of(
                "diagEsperado", "A relacao clinica precisa ser confirmada"));
        when(aiClient.gerarConteudo(any(), any()))
                .thenReturn(
                        primeiraAvaliacao,
                        validacaoCoerencia("COERENTE"),
                        respostaIa(),
                        validacaoCoerencia("COERENTE"));
        when(casoRepository.findById(1L)).thenReturn(Optional.of(criarCaso()));
        when(pacienteRepository.findByCasoClinicoIdCasoOrderByIdPacienteAsc(1L))
                .thenReturn(List.of());
        when(conteudoRepository.save(any(ConteudoClinico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CasoClinicoIaResponseDTO resposta = servico.gerarConteudo(
                1L,
                new CasoClinicoIaRequestDTO(
                        null,
                        null,
                        null,
                        null,
                        "Exacerbacao asmatica"));

        assertThat(resposta.getDiagEsperado()).isEqualTo("Exacerbacao asmatica");
        verify(aiClient, times(4)).gerarConteudo(any(), any());
        verify(conteudoRepository).save(any(ConteudoClinico.class));
    }

    @Test
    void deveIgnorarViolacoesQueTratamCamposAusentesComoIncoerentes() {
        ServicoCasoClinicoIa servico = servicoComChave();
        CasoClinicoGeradoIaDTO falsoPositivo = validacaoCoerencia("INCOERENTE");
        falsoPositivo.setViolacoes(Map.of(
                "sintomas", "informacao ausente",
                "contexto", "informacao ausente",
                "examClinico", "informacao ausente",
                "antecClinico", "informacao ausente"));
        CasoClinicoGeradoIaDTO gerado = respostaIa();
        gerado.setSintomas("Palpitacoes, fadiga progressiva e intolerancia aos esforcos");
        gerado.setContexto("Paciente procura atendimento por piora gradual das palpitacoes");
        gerado.setExamClinico("Pulso irregular e ECG com ritmo irregularmente irregular");
        gerado.setAntecClinico("Hipertensao arterial sistemica");
        gerado.setDiagEsperado("Fibrilacao Atrial");
        when(aiClient.gerarConteudo(any(), any())).thenReturn(
                falsoPositivo,
                falsoPositivo,
                gerado,
                validacaoCoerencia("COERENTE"));

        CasoClinico caso = criarCaso();
        caso.setTitulo("Palpitacoes e Cansaco Progressivo");
        caso.setDisciplina("Clinica Medica");
        caso.setEspecialidade("Cardiologia");
        caso.setObjetivoAprendizagem(
                "Diferenciar causas de palpitacao e interpretar ritmo cardiaco no ECG");
        when(casoRepository.findById(1L)).thenReturn(Optional.of(caso));
        when(pacienteRepository.findByCasoClinicoIdCasoOrderByIdPacienteAsc(1L))
                .thenReturn(List.of());
        when(conteudoRepository.save(any(ConteudoClinico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CasoClinicoIaResponseDTO resposta = servico.gerarConteudo(
                1L,
                new CasoClinicoIaRequestDTO(null, null, null, null, "Fibrilacao Atrial"));

        assertThat(resposta.getSintomas()).isEqualTo(gerado.getSintomas());
        assertThat(resposta.getContexto()).isEqualTo(gerado.getContexto());
        assertThat(resposta.getExamClinico()).isEqualTo(gerado.getExamClinico());
        assertThat(resposta.getAntecClinico()).isEqualTo(gerado.getAntecClinico());
        ArgumentCaptor<String> contextos = ArgumentCaptor.forClass(String.class);
        verify(aiClient, times(4)).gerarConteudo(any(), contextos.capture());
        assertThat(contextos.getAllValues().getFirst())
                .contains(
                        "titulo: Palpitacoes e Cansaco Progressivo",
                        "disciplina: Clinica Medica",
                        "especialidade: Cardiologia",
                        "objetivoAprendizagem: Diferenciar causas de palpitacao",
                        "diagEsperado: Fibrilacao Atrial",
                        "<campos_com_dados_informados>")
                .doesNotContain(
                        "sintomas: informacao ausente",
                        "contexto: informacao ausente",
                        "examClinico: informacao ausente",
                        "antecClinico: informacao ausente");
    }

    @Test
    void deveProsseguirQuandoPreValidacaoFicaIncertaSemContradicaoConfirmada() {
        ServicoCasoClinicoIa servico = servicoComChave();
        CasoClinicoGeradoIaDTO incerta = validacaoCoerencia("INCERTO");
        incerta.setViolacoes(Map.of(
                "objetivoAprendizagem", "Detalhe como o objetivo se relaciona ao diagnostico"));
        when(aiClient.gerarConteudo(any(), any())).thenReturn(
                incerta,
                incerta,
                respostaIa(),
                validacaoCoerencia("COERENTE"));
        when(casoRepository.findById(1L)).thenReturn(Optional.of(criarCaso()));
        when(pacienteRepository.findByCasoClinicoIdCasoOrderByIdPacienteAsc(1L))
                .thenReturn(List.of());
        when(conteudoRepository.save(any(ConteudoClinico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CasoClinicoIaResponseDTO resposta = servico.gerarConteudo(
                1L,
                new CasoClinicoIaRequestDTO(
                        null,
                        null,
                        null,
                        null,
                        "Exacerbacao asmatica"));

        assertThat(resposta.getSintomas()).isEqualTo("Febre gerada pela IA");
        verify(aiClient, times(4)).gerarConteudo(any(), any());
        verify(transactionService).executarGeracao(any(), any(), any(), any());
    }

    @Test
    void deveBloquearAncorasAusentesSemChamarIa() {
        ServicoCasoClinicoIa servico = servicoComChave();
        CasoClinico caso = criarCaso();
        caso.setEspecialidade(" ");
        caso.setObjetivoAprendizagem(null);
        when(casoRepository.findById(1L)).thenReturn(Optional.of(caso));
        when(pacienteRepository.findByCasoClinicoIdCasoOrderByIdPacienteAsc(1L))
                .thenReturn(List.of());

        assertThatThrownBy(() -> servico.gerarConteudo(
                1L,
                new CasoClinicoIaRequestDTO(null, null, null, null, null)))
                .isInstanceOfSatisfying(CoerenciaCasoClinicoException.class, erro ->
                        assertThat(erro.getCampos())
                                .containsKeys("especialidade", "diagEsperado", "objetivoAprendizagem"));
        verify(aiClient, never()).gerarConteudo(any(), any());
        verify(transactionService, never()).executarGeracao(any(), any(), any(), any());
    }

    @Test
    void devePreValidarMesmoQuandoTodosOsCamposForamPreenchidosPeloProfessor() {
        ServicoCasoClinicoIa servico = servicoComChave();
        when(aiClient.gerarConteudo(any(), any())).thenReturn(validacaoCoerencia("COERENTE"));
        when(casoRepository.findById(1L)).thenReturn(Optional.of(criarCaso()));
        when(pacienteRepository.findByCasoClinicoIdCasoOrderByIdPacienteAsc(1L))
                .thenReturn(List.of());
        when(conteudoRepository.save(any(ConteudoClinico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        CasoClinicoIaRequestDTO requisicao = new CasoClinicoIaRequestDTO(
                "Sintoma literal",
                "Contexto literal",
                "Exame literal",
                "Antecedente literal",
                "Diagnostico literal");

        CasoClinicoIaResponseDTO resposta = servico.gerarConteudo(1L, requisicao);

        assertThat(resposta.getSintomas()).isEqualTo("Sintoma literal");
        assertThat(resposta.getContexto()).isEqualTo("Contexto literal");
        assertThat(resposta.getExamClinico()).isEqualTo("Exame literal");
        assertThat(resposta.getAntecClinico()).isEqualTo("Antecedente literal");
        assertThat(resposta.getDiagEsperado()).isEqualTo("Diagnostico literal");
        verify(aiClient).gerarConteudo(any(), any());
    }

    @Test
    void deveRetornarCamposEstruturadosQuandoConteudoGeradoForIncoerente() {
        ServicoCasoClinicoIa servico = servicoComChave();
        CasoClinicoGeradoIaDTO incoerente = validacaoCoerencia("INCOERENTE");
        incoerente.setViolacoes(Map.of(
                "sintomas", "Os sintomas gerados nao sustentam o diagnostico esperado",
                "diagEsperado", "O diagnostico nao e sustentado pelos sintomas gerados"));
        when(aiClient.gerarConteudo(any(), any()))
                .thenReturn(validacaoCoerencia("COERENTE"), respostaIa(), incoerente);
        when(casoRepository.findById(1L)).thenReturn(Optional.of(criarCaso()));
        when(pacienteRepository.findByCasoClinicoIdCasoOrderByIdPacienteAsc(1L))
                .thenReturn(List.of());

        assertThatThrownBy(() -> servico.gerarConteudo(
                1L,
                new CasoClinicoIaRequestDTO(
                        null,
                        null,
                        null,
                        null,
                        "Exacerbacao asmatica")))
                .isInstanceOfSatisfying(CoerenciaCasoClinicoException.class, erro ->
                        assertThat(erro.getCampos()).containsKeys("sintomas", "diagEsperado"));
        verify(transactionService, never()).executarGeracao(any(), any(), any(), any());
        verify(conteudoRepository, never()).save(any(ConteudoClinico.class));
    }

    @Test
    void deveFalharQuandoValidadorNaoRetornaStatusDeCoerencia() {
        ServicoCasoClinicoIa servico = servicoComChave();
        when(aiClient.gerarConteudo(any(), any()))
                .thenReturn(new CasoClinicoGeradoIaDTO());

        when(casoRepository.findById(1L)).thenReturn(Optional.of(criarCaso()));
        when(pacienteRepository.findByCasoClinicoIdCasoOrderByIdPacienteAsc(1L))
                .thenReturn(List.of());

        CasoClinicoIaRequestDTO requisicao = new CasoClinicoIaRequestDTO(
                null,
                null,
                null,
                null,
                "Exacerbacao asmatica");

        assertThatThrownBy(() -> servico.gerarConteudo(1L, requisicao))
                .isInstanceOf(com.SistemaApiCrud.SistemaCrud.exception.AiProviderException.class)
                .hasMessageContaining("nao avaliou a coerencia");
        verify(transactionService, never()).executarGeracao(any(), any(), any(), any());
    }

    @Test
    void deveExigirChaveIaQuandoExistemCamposParaGerar() {
        ServicoCasoClinicoIa servico = new ServicoCasoClinicoIa(
                aiClient,
                casoRepository,
                conteudoRepository,
                pacienteRepository,
                transactionService,
                auditService,
                new ProtecaoDadosClinicosIa(),
                "");

        when(casoRepository.findById(1L)).thenReturn(Optional.of(criarCaso()));

        CasoClinicoIaRequestDTO requisicao = new CasoClinicoIaRequestDTO(
                "Sintoma informado",
                null,
                "Exame informado",
                "Antecedente informado",
                "Diagnostico informado");

        assertThatThrownBy(() -> servico.gerarConteudo(1L, requisicao))
                .isInstanceOf(ServicoIndisponivelException.class)
                .hasMessage("Configure a variavel IA_CHAVE_API antes de gerar conteudo com IA");
    }

    @Test
    void deveAjustarConteudoClinicoExistenteComIa() {
        ServicoCasoClinicoIa servico = servicoComChave();
        when(aiClient.gerarConteudo(any(), any())).thenReturn(respostaIa());

        CasoClinico caso = criarCaso();
        caso.setTitulo("Atendimento ao queimado revisado");
        caso.setEspecialidade("Atendimento ao Queimado");
        ConteudoClinico conteudoAtual = new ConteudoClinico();
        conteudoAtual.setIdConteudo(22L);
        conteudoAtual.setCasoClinico(caso);
        conteudoAtual.setSintomas("Dispneia e chiado");
        conteudoAtual.setContexto("Paciente procura atendimento");
        conteudoAtual.setExamClinico("Sibilos difusos");
        conteudoAtual.setAntecClinico("Asma previa");
        conteudoAtual.setDiagEsperado("Exacerbacao asmatica");

        Paciente pacienteAtualizado = new Paciente();
        pacienteAtualizado.setIdPaciente(9L);
        pacienteAtualizado.setCasoClinico(caso);
        pacienteAtualizado.setNome("Paciente simulado");
        pacienteAtualizado.setIdade(32);
        pacienteAtualizado.setSexo(Sexo.MASCULINO);
        pacienteAtualizado.setEstadoCivil(EstadoCivil.CASADO);
        pacienteAtualizado.setProfissao("Professor");
        pacienteAtualizado.setPeso("75 kg");
        pacienteAtualizado.setAltura("178 cm");

        when(casoRepository.findById(1L)).thenReturn(Optional.of(caso));
        when(pacienteRepository.findByCasoClinicoIdCasoOrderByIdPacienteAsc(1L))
                .thenReturn(List.of(pacienteAtualizado));
        when(conteudoRepository.findFirstByCasoClinicoIdCasoOrderByIdConteudoDesc(1L))
                .thenReturn(Optional.of(conteudoAtual));
        when(conteudoRepository.save(any(ConteudoClinico.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CasoClinicoIaResponseDTO resposta = servico.ajustarConteudo(
                1L,
                new CasoClinicoAjusteRequestDTO("SIMPLIFICAR", null, true));

        assertThat(resposta.getIdConteudo()).isEqualTo(22L);
        assertThat(resposta.getContexto()).isEqualTo("Paciente adulto em atendimento ambulatorial");
        assertThat(conteudoAtual.getExamClinico()).isEqualTo("Ausculta pulmonar com sibilos");

        ArgumentCaptor<String> contexto = ArgumentCaptor.forClass(String.class);
        verify(aiClient, times(3)).gerarConteudo(any(), contexto.capture());
        assertThat(contexto.getAllValues().get(1)).contains(
                "titulo: Atendimento ao queimado revisado",
                "especialidade: Atendimento ao Queimado",
                "idade: 32",
                "sexo: MASCULINO",
                "estadoCivil: CASADO",
                "profissao: Professor",
                "peso: 75 kg",
                "altura: 178 cm",
                "diagEsperado: Exacerbacao asmatica");
    }

    @Test
    void deveBloquearAjusteQuandoConteudoAtualForConfirmadoComoIncoerente() {
        ServicoCasoClinicoIa servico = servicoComChave();
        CasoClinico caso = criarCaso();
        ConteudoClinico atual = conteudoAtual(caso);
        CasoClinicoGeradoIaDTO incoerente = validacaoCoerencia("INCOERENTE");
        incoerente.setViolacoes(Map.of(
                "diagEsperado",
                "O diagnostico nao corresponde aos sintomas informados"));
        when(aiClient.gerarConteudo(any(), any())).thenReturn(incoerente, incoerente);
        when(casoRepository.findById(1L)).thenReturn(Optional.of(caso));
        when(conteudoRepository.findFirstByCasoClinicoIdCasoOrderByIdConteudoDesc(1L))
                .thenReturn(Optional.of(atual));
        when(pacienteRepository.findByCasoClinicoIdCasoOrderByIdPacienteAsc(1L))
                .thenReturn(List.of());

        assertThatThrownBy(() -> servico.ajustarConteudo(
                1L,
                new CasoClinicoAjusteRequestDTO("SIMPLIFICAR", null, true)))
                .isInstanceOfSatisfying(CoerenciaCasoClinicoException.class, erro -> assertThat(
                        erro.getCampos()).containsKey("diagEsperado"));

        verify(aiClient, times(2)).gerarConteudo(any(), any());
        verify(transactionService, never()).executarAjuste(any(), any(), any(), any(), any());
    }

    @Test
    void deveAtualizarPacienteSemAlterarObjetivoInformadoPeloProfessor() {
        ServicoCasoClinicoIa servico = servicoComChave();
        when(aiClient.gerarConteudo(any(), any()))
                .thenReturn(
                        validacaoCoerencia("COERENTE"),
                        respostaIaComComplementos(),
                        validacaoCoerencia("COERENTE"));

        CasoClinico caso = criarCaso();

        Paciente paciente = new Paciente();
        paciente.setIdPaciente(5L);
        paciente.setCasoClinico(caso);
        paciente.setNome("Maria Sigilosa");
        paciente.setIdade(0);
        paciente.setSexo(Sexo.NAO_INFORMADO);
        paciente.setEstadoCivil(EstadoCivil.NAO_INFORMADO);
        paciente.setProfissao("NAO_INFORMADO");
        paciente.setPeso("NAO_INFORMADO");
        paciente.setAltura("NAO_INFORMADO");

        when(casoRepository.findById(1L)).thenReturn(Optional.of(caso));
        when(casoRepository.save(any(CasoClinico.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pacienteRepository.findByCasoClinicoIdCasoOrderByIdPacienteAsc(1L))
                .thenReturn(List.of(paciente));
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conteudoRepository.save(any(ConteudoClinico.class))).thenAnswer(invocation -> {
            ConteudoClinico conteudo = invocation.getArgument(0);
            conteudo.setIdConteudo(33L);
            return conteudo;
        });

        CasoClinicoIaRequestDTO requisicao = new CasoClinicoIaRequestDTO(
                null,
                null,
                null,
                null,
                "Exacerbacao asmatica");
        requisicao.setDadosSinteticosOuDesidentificados(true);
        requisicao.setPermitirComplementoIa(true);
        requisicao.setInformacoesAdicionaisPaciente(
                "CPF: 123.456.789-00; outro CPF 98765432100; "
                        + "CNS 123456789012345; e-mail: maria.sigilo@example.com");

        CasoClinicoIaResponseDTO resposta = servico.gerarConteudo(1L, requisicao);

        assertThat(resposta.getIdConteudo()).isEqualTo(33L);
        assertThat(caso.getObjetivoAprendizagem()).isEqualTo("Avaliar conduta respiratoria");
        assertThat(paciente.getNome()).isEqualTo("Maria Sigilosa");
        assertThat(paciente.getIdade()).isEqualTo(45);
        assertThat(paciente.getSexo()).isEqualTo(Sexo.MASCULINO);
        assertThat(paciente.getEstadoCivil()).isEqualTo(EstadoCivil.CASADO);
        assertThat(paciente.getProfissao()).isEqualTo("Professor");
        assertThat(paciente.getPeso()).isEqualTo("80 kg");
        assertThat(paciente.getAltura()).isEqualTo("170 cm");

        ArgumentCaptor<String> instrucoesSistema = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> contexto = ArgumentCaptor.forClass(String.class);
        verify(aiClient, times(3)).gerarConteudo(instrucoesSistema.capture(), contexto.capture());
        assertThat(instrucoesSistema.getAllValues().get(1))
                .contains(
                        "restricoes tematicas obrigatorias",
                        "Nao escolha uma restricao ignorando outra",
                        "dados nao confiaveis");
        assertThat(contexto.getAllValues().get(1))
                .contains(
                        "[DADO_REMOVIDO]",
                        "titulo: Caso respiratorio",
                        "disciplina: Clinica Medica",
                        "especialidade: Pneumologia",
                        "{\"statusCoerencia\":\"\"")
                .doesNotContain(
                        "Maria Sigilosa",
                        "123.456.789-00",
                        "98765432100",
                        "123456789012345",
                        "maria.sigilo@example.com");
        assertThat(instrucoesSistema.getAllValues().get(2))
                .contains("revisor clinico independente", "Use INCOERENTE", "Use INCERTO");
        assertThat(contexto.getAllValues().get(2))
                .contains(
                        "<restricoes_obrigatorias>",
                        "especialidade: Pneumologia",
                        "<conteudo_candidato>",
                        "diagEsperado: Exacerbacao asmatica");
    }

    @Test
    void devePreservarPacienteQuandoAjusteTentaMudarDadosCadastrais() {
        ServicoCasoClinicoIa servico = servicoComChave();
        when(aiClient.gerarConteudo(any(), any())).thenReturn(respostaIaAjustandoPaciente());

        CasoClinico caso = criarCaso();
        ConteudoClinico conteudoAtual = new ConteudoClinico();
        conteudoAtual.setIdConteudo(44L);
        conteudoAtual.setCasoClinico(caso);
        conteudoAtual.setSintomas("Dor no peito e falta de ar");
        conteudoAtual.setContexto("Paciente de 55 anos com quadro cardiologico");
        conteudoAtual.setExamClinico("ECG com alteracoes isquemicas");
        conteudoAtual.setAntecClinico("Hipertensao");
        conteudoAtual.setDiagEsperado("Angina instavel");

        Paciente paciente = new Paciente();
        paciente.setIdPaciente(7L);
        paciente.setCasoClinico(caso);
        paciente.setNome("João Silva");
        paciente.setIdade(55);
        paciente.setSexo(Sexo.MASCULINO);
        paciente.setEstadoCivil(EstadoCivil.CASADO);
        paciente.setProfissao("Engenheiro");
        paciente.setPeso("80kg");
        paciente.setAltura("1.75m");

        when(casoRepository.findById(1L)).thenReturn(Optional.of(caso));
        when(casoRepository.save(any(CasoClinico.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pacienteRepository.findByCasoClinicoIdCasoOrderByIdPacienteAsc(1L))
                .thenReturn(List.of(paciente));
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conteudoRepository.findFirstByCasoClinicoIdCasoOrderByIdConteudoDesc(1L))
                .thenReturn(Optional.of(conteudoAtual));
        when(conteudoRepository.save(any(ConteudoClinico.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CasoClinicoIaResponseDTO resposta = servico.ajustarConteudo(
                1L,
                new CasoClinicoAjusteRequestDTO(
                        "PERSONALIZADO",
                        "Paciente precisa ter menos que 25 anos",
                        true));

        assertThat(resposta.getIdConteudo()).isEqualTo(44L);
        assertThat(resposta.getDiagEsperado()).isEqualTo("Angina instavel");
        assertThat(conteudoAtual.getDiagEsperado()).isEqualTo("Angina instavel");
        assertThat(paciente.getIdade()).isEqualTo(55);
        assertThat(caso.getObjetivoAprendizagem()).isEqualTo("Avaliar conduta respiratoria");
    }

    @Test
    void deveRecuperarSintomasAusentesComUmaChamadaFocada() {
        ServicoCasoClinicoIa servico = servicoComChave();
        CasoClinicoGeradoIaDTO incompleta = respostaIa();
        incompleta.setSintomas(null);
        when(aiClient.gerarConteudo(any(), any()))
                .thenReturn(
                        validacaoCoerencia("COERENTE"),
                        incompleta,
                        respostaIa(),
                        validacaoCoerencia("COERENTE"));
        when(casoRepository.findById(1L)).thenReturn(Optional.of(criarCaso()));
        when(pacienteRepository.findByCasoClinicoIdCasoOrderByIdPacienteAsc(1L)).thenReturn(List.of());
        when(conteudoRepository.save(any(ConteudoClinico.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CasoClinicoIaResponseDTO resposta = servico.gerarConteudo(
                1L,
                new CasoClinicoIaRequestDTO(null, null, null, null, "Exacerbacao asmatica"));

        assertThat(resposta.getSintomas()).isEqualTo("Febre gerada pela IA");
        ArgumentCaptor<String> instrucoes = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> contextos = ArgumentCaptor.forClass(String.class);
        verify(aiClient, times(4)).gerarConteudo(instrucoes.capture(), contextos.capture());
        assertThat(instrucoes.getAllValues().get(2)).contains("recuperacao_obrigatoria", "sintomas");
        assertThat(contextos.getAllValues().get(2)).isEqualTo(contextos.getAllValues().get(1));
    }

    @Test
    void deveReutilizarContextoLimitadoNoReparoSemDuplicarCamposDoProfessor() {
        ServicoCasoClinicoIa servico = servicoComChave();
        CasoClinicoGeradoIaDTO incompleta = respostaIa();
        incompleta.setAntecClinico(null);
        when(aiClient.gerarConteudo(any(), any()))
                .thenReturn(
                        validacaoCoerencia("COERENTE"),
                        incompleta,
                        respostaIa(),
                        validacaoCoerencia("COERENTE"));
        when(casoRepository.findById(1L)).thenReturn(Optional.of(criarCaso()));
        when(pacienteRepository.findByCasoClinicoIdCasoOrderByIdPacienteAsc(1L))
                .thenReturn(List.of());
        when(conteudoRepository.save(any(ConteudoClinico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        String campoLongo = "x".repeat(10_000);

        servico.gerarConteudo(
                1L,
                new CasoClinicoIaRequestDTO(
                        campoLongo,
                        campoLongo,
                        campoLongo,
                        null,
                        "Exacerbacao asmatica"));

        ArgumentCaptor<String> contextos = ArgumentCaptor.forClass(String.class);
        verify(aiClient, times(4)).gerarConteudo(any(), contextos.capture());
        assertThat(contextos.getAllValues())
                .allSatisfy(contexto -> assertThat(contexto.length()).isLessThanOrEqualTo(40_000));
        assertThat(contextos.getAllValues().get(3))
                .contains("camposClinicosFornecidosProfessor: sintomas, contexto, examClinico, diagEsperado")
                .doesNotContain("sintomasProfessor", "contextoProfessor", "examClinicoProfessor");
    }

    @Test
    void deveFalharSemPersistirQuandoRecuperacaoAindaEstaIncompleta() {
        ServicoCasoClinicoIa servico = servicoComChave();
        CasoClinicoGeradoIaDTO incompleta = respostaIa();
        incompleta.setSintomas(null);
        CasoClinicoGeradoIaDTO aindaIncompleta = respostaIa();
        aindaIncompleta.setSintomas(" ");
        when(aiClient.gerarConteudo(any(), any()))
                .thenReturn(validacaoCoerencia("COERENTE"), incompleta, aindaIncompleta);
        when(casoRepository.findById(1L)).thenReturn(Optional.of(criarCaso()));
        when(pacienteRepository.findByCasoClinicoIdCasoOrderByIdPacienteAsc(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> servico.gerarConteudo(
                1L,
                new CasoClinicoIaRequestDTO(null, null, null, null, "Exacerbacao asmatica")))
                .isInstanceOf(com.SistemaApiCrud.SistemaCrud.exception.AiProviderException.class)
                .hasMessageContaining("sintomas");
        verify(transactionService, never()).executarGeracao(any(), any(), any(), any());
        verify(aiClient, times(3)).gerarConteudo(any(), any());
    }

    @Test
    void deveIgnorarSombraInvalidaEmCampoFornecidoPeloProfessor() {
        ServicoCasoClinicoIa servico = servicoComChave();
        CasoClinicoGeradoIaDTO gerado = respostaIa();
        gerado.setSintomas("x".repeat(10_001));
        when(aiClient.gerarConteudo(any(), any()))
                .thenReturn(
                        validacaoCoerencia("COERENTE"),
                        gerado,
                        validacaoCoerencia("COERENTE"));
        when(casoRepository.findById(1L)).thenReturn(Optional.of(criarCaso()));
        when(pacienteRepository.findByCasoClinicoIdCasoOrderByIdPacienteAsc(1L)).thenReturn(List.of());
        when(conteudoRepository.save(any(ConteudoClinico.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CasoClinicoIaResponseDTO resposta = servico.gerarConteudo(
                1L,
                new CasoClinicoIaRequestDTO(
                        "Sintoma do professor",
                        null,
                        null,
                        null,
                        "Exacerbacao asmatica"));

        assertThat(resposta.getSintomas()).isEqualTo("Sintoma do professor");
        verify(aiClient, times(3)).gerarConteudo(any(), any());
    }

    @Test
    void deveManterConteudoAnteriorQuandoAjusteRetornaCampoInvalido() {
        ServicoCasoClinicoIa servico = servicoComChave();
        CasoClinico caso = criarCaso();
        ConteudoClinico atual = conteudoAtual(caso);
        CasoClinicoGeradoIaDTO parcial = respostaIa();
        parcial.setSintomas(null);
        parcial.setContexto(" ");
        parcial.setDiagEsperado("Outro diagnostico");
        when(aiClient.gerarConteudo(any(), any())).thenReturn(parcial);
        when(casoRepository.findById(1L)).thenReturn(Optional.of(caso));
        when(conteudoRepository.findFirstByCasoClinicoIdCasoOrderByIdConteudoDesc(1L))
                .thenReturn(Optional.of(atual));
        when(pacienteRepository.findByCasoClinicoIdCasoOrderByIdPacienteAsc(1L)).thenReturn(List.of());
        when(conteudoRepository.save(any(ConteudoClinico.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CasoClinicoIaResponseDTO resposta = servico.ajustarConteudo(
                1L,
                new CasoClinicoAjusteRequestDTO("SIMPLIFICAR", null, true));

        assertThat(resposta.getSintomas()).isEqualTo(atual.getSintomas());
        assertThat(resposta.getContexto()).isEqualTo(atual.getContexto());
        assertThat(resposta.getDiagEsperado()).isEqualTo(atual.getDiagEsperado());
    }

    @Test
    void deveFalharSemPersistirQuandoAjusteNaoRetornaNenhumCampoValido() {
        ServicoCasoClinicoIa servico = servicoComChave();
        CasoClinico caso = criarCaso();
        ConteudoClinico atual = conteudoAtual(caso);
        when(aiClient.gerarConteudo(any(), any())).thenReturn(
                validacaoCoerencia("COERENTE"),
                new CasoClinicoGeradoIaDTO());
        when(casoRepository.findById(1L)).thenReturn(Optional.of(caso));
        when(conteudoRepository.findFirstByCasoClinicoIdCasoOrderByIdConteudoDesc(1L))
                .thenReturn(Optional.of(atual));
        when(pacienteRepository.findByCasoClinicoIdCasoOrderByIdPacienteAsc(1L))
                .thenReturn(List.of());

        assertThatThrownBy(() -> servico.ajustarConteudo(
                1L,
                new CasoClinicoAjusteRequestDTO("SIMPLIFICAR", null, true)))
                .isInstanceOf(com.SistemaApiCrud.SistemaCrud.exception.AiProviderException.class)
                .hasMessageContaining("nenhum campo clinico valido");
        verify(transactionService, never()).executarAjuste(any(), any(), any(), any(), any());
        verify(conteudoRepository, never()).save(any(ConteudoClinico.class));
    }

    @Test
    void deveIgnorarComplementoCadastralInvalidoSemCancelarConteudo() {
        ServicoCasoClinicoIa servico = servicoComChave();
        CasoClinico caso = criarCaso();
        Paciente paciente = new Paciente();
        paciente.setIdPaciente(7L);
        paciente.setCasoClinico(caso);
        paciente.setIdade(0);
        paciente.setSexo(Sexo.NAO_INFORMADO);
        paciente.setEstadoCivil(EstadoCivil.NAO_INFORMADO);
        CasoClinicoGeradoIaDTO gerado = respostaIa();
        gerado.setPaciente(pacienteGerado(200, "DESCONHECIDO", "INVALIDO", "p".repeat(121), "x".repeat(21), "x".repeat(21)));
        when(aiClient.gerarConteudo(any(), any()))
                .thenReturn(
                        validacaoCoerencia("COERENTE"),
                        gerado,
                        validacaoCoerencia("COERENTE"));
        when(casoRepository.findById(1L)).thenReturn(Optional.of(caso));
        when(pacienteRepository.findByCasoClinicoIdCasoOrderByIdPacienteAsc(1L)).thenReturn(List.of(paciente));
        when(conteudoRepository.save(any(ConteudoClinico.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CasoClinicoIaRequestDTO requisicao = new CasoClinicoIaRequestDTO(
                null,
                null,
                null,
                null,
                "Exacerbacao asmatica");
        requisicao.setPermitirComplementoIa(true);

        CasoClinicoIaResponseDTO resposta = servico.gerarConteudo(1L, requisicao);

        assertThat(resposta.getSintomas()).isEqualTo("Febre gerada pela IA");
        assertThat(paciente.getIdade()).isZero();
        assertThat(paciente.getSexo()).isEqualTo(Sexo.NAO_INFORMADO);
        verify(pacienteRepository, never()).save(any(Paciente.class));
    }

    @Test
    void deveRecuperarUmaVezStatusDeCoerenciaAusente() {
        ServicoCasoClinicoIa servico = servicoComChave();
        when(aiClient.gerarConteudo(any(), any()))
                .thenReturn(
                        validacaoCoerencia("COERENTE"),
                        respostaIa(),
                        new CasoClinicoGeradoIaDTO(),
                        validacaoCoerencia("COERENTE"));
        when(casoRepository.findById(1L)).thenReturn(Optional.of(criarCaso()));
        when(pacienteRepository.findByCasoClinicoIdCasoOrderByIdPacienteAsc(1L)).thenReturn(List.of());
        when(conteudoRepository.save(any(ConteudoClinico.class))).thenAnswer(invocation -> invocation.getArgument(0));

        servico.gerarConteudo(1L, new CasoClinicoIaRequestDTO(null, null, null, null, "Exacerbacao asmatica"));
        verify(aiClient, times(4)).gerarConteudo(any(), any());
    }

    @Test
    void deveFalharComSegurancaQuandoAsDuasValidacoesDeCoerenciaSaoInvalidas() {
        ServicoCasoClinicoIa servico = servicoComChave();
        when(aiClient.gerarConteudo(any(), any()))
                .thenReturn(
                        validacaoCoerencia("COERENTE"),
                        respostaIa(),
                        new CasoClinicoGeradoIaDTO(),
                        new CasoClinicoGeradoIaDTO());
        when(casoRepository.findById(1L)).thenReturn(Optional.of(criarCaso()));
        when(pacienteRepository.findByCasoClinicoIdCasoOrderByIdPacienteAsc(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> servico.gerarConteudo(
                1L,
                new CasoClinicoIaRequestDTO(null, null, null, null, "Exacerbacao asmatica")))
                .isInstanceOf(com.SistemaApiCrud.SistemaCrud.exception.AiProviderException.class)
                .hasMessageContaining("coerencia");
        verify(transactionService, never()).executarGeracao(any(), any(), any(), any());
    }

    private ConteudoClinico conteudoAtual(CasoClinico caso) {
        ConteudoClinico conteudo = new ConteudoClinico();
        conteudo.setIdConteudo(90L);
        conteudo.setCasoClinico(caso);
        conteudo.setSintomas("Sintomas anteriores");
        conteudo.setContexto("Contexto anterior");
        conteudo.setExamClinico("Exame anterior");
        conteudo.setAntecClinico("Antecedente anterior");
        conteudo.setDiagEsperado("Diagnostico anterior");
        return conteudo;
    }

    private ServicoCasoClinicoIa servicoComChave() {
        configurarExecutorTransacional();
        return new ServicoCasoClinicoIa(
                aiClient,
                casoRepository,
                conteudoRepository,
                pacienteRepository,
                transactionService,
                auditService,
                new ProtecaoDadosClinicosIa(),
                "chave-teste");
    }

    @SuppressWarnings("unchecked")
    private void configurarExecutorTransacional() {
        when(transactionService.executarGeracao(any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    Long idCaso = invocation.getArgument(0);
                    Function<CasoClinico, CasoClinicoIaResponseDTO> operacao =
                            invocation.getArgument(3);
                    CasoClinico caso = casoRepository.findById(idCaso).orElseThrow();
                    return operacao.apply(caso);
                });
        when(transactionService.executarAjuste(any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    Long idCaso = invocation.getArgument(0);
                    BiFunction<CasoClinico, ConteudoClinico, CasoClinicoIaResponseDTO> operacao =
                            invocation.getArgument(4);
                    CasoClinico caso = casoRepository.findById(idCaso).orElseThrow();
                    ConteudoClinico conteudo = conteudoRepository
                            .findFirstByCasoClinicoIdCasoOrderByIdConteudoDesc(idCaso)
                            .orElseThrow();
                    return operacao.apply(caso, conteudo);
                });
    }

    private CasoClinicoGeradoIaDTO respostaIa() {
        CasoClinicoGeradoIaDTO resposta = new CasoClinicoGeradoIaDTO();
        resposta.setStatusCoerencia("COERENTE");
        resposta.setSintomas("Febre gerada pela IA");
        resposta.setContexto("Paciente adulto em atendimento ambulatorial");
        resposta.setExamClinico("Ausculta pulmonar com sibilos");
        resposta.setAntecClinico("Historico de asma");
        resposta.setDiagEsperado("Exacerbacao asmatica");
        return resposta;
    }

    private CasoClinicoGeradoIaDTO validacaoCoerencia(String status) {
        CasoClinicoGeradoIaDTO validacao = new CasoClinicoGeradoIaDTO();
        validacao.setStatusCoerencia(status);
        return validacao;
    }

    private CasoClinicoGeradoIaDTO respostaIaComComplementos() {
        CasoClinicoGeradoIaDTO resposta = respostaIa();
        resposta.setSintomas("Dispneia e chiado");
        resposta.setContexto("Paciente procura atendimento por piora respiratoria");
        resposta.setExamClinico("Sibilos difusos");
        resposta.setObjetivoAprendizagem("Identificar sinais de exacerbação asmática e definir conduta inicial.");
        resposta.setPaciente(pacienteGerado(
                45,
                "MASCULINO",
                "CASADO",
                "Professor",
                "80 kg",
                "170 cm"));
        return resposta;
    }

    private CasoClinicoGeradoIaDTO respostaIaAjustandoPaciente() {
        CasoClinicoGeradoIaDTO resposta = respostaIa();
        resposta.setSintomas("Dor no peito intermitente e falta de ar aos esforços");
        resposta.setContexto("Paciente João Silva, 22 anos, sexo masculino, casado, engenheiro, com peso de 80kg e altura de 1.75m.");
        resposta.setExamClinico("ECG com alteracoes isquemicas");
        resposta.setAntecClinico("Hipertensao");
        resposta.setDiagEsperado("Infarto agudo do miocardio");
        resposta.setObjetivoAprendizagem("Avaliar conduta respiratoria");
        resposta.setPaciente(pacienteGerado(
                22,
                "MASCULINO",
                "CASADO",
                "Engenheiro",
                "80kg",
                "1.75m"));
        return resposta;
    }

    private PacienteGeradoIaDTO pacienteGerado(
            Integer idade,
            String sexo,
            String estadoCivil,
            String profissao,
            String peso,
            String altura) {
        PacienteGeradoIaDTO paciente = new PacienteGeradoIaDTO();
        paciente.setIdade(idade);
        paciente.setSexo(sexo);
        paciente.setEstadoCivil(estadoCivil);
        paciente.setProfissao(profissao);
        paciente.setPeso(peso);
        paciente.setAltura(altura);
        return paciente;
    }

    private CasoClinico criarCaso() {
        CasoClinico caso = new CasoClinico();
        caso.setIdCaso(1L);
        caso.setStatus(StatusCasoClinico.RASCUNHO);
        caso.setProfessor(new Professor(1L, "Dra. Ana", "ana@email.com", "Clinica"));
        caso.setTitulo("Caso respiratorio");
        caso.setNivelDificuldade(NivelDificuldade.MEDIA);
        caso.setDisciplina("Clinica Medica");
        caso.setAreaSaude("Medicina");
        caso.setEstilo("Caso guiado");
        caso.setEspecialidade("Pneumologia");
        caso.setObjetivoAprendizagem("Avaliar conduta respiratoria");
        return caso;
    }
}
