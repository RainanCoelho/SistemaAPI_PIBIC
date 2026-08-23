package com.SistemaApiCrud.SistemaCrud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
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
                .isInstanceOf(com.SistemaApiCrud.SistemaCrud.exception.BusinessException.class)
                .hasMessageContaining("sinteticos ou foram desidentificados");
    }

    @Test
    void deveCompletarCamposVaziosComRespostaDaIaEPreservarDadosDoProfessor() {
        ServicoCasoClinicoIa servico = servicoComChave();
        when(aiClient.gerarConteudo(any(), any())).thenReturn(respostaIa());

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
                null);

        CasoClinicoIaResponseDTO resposta = servico.gerarConteudo(1L, requisicao);

        assertThat(resposta.getIdConteudo()).isEqualTo(10L);
        assertThat(resposta.getIdCaso()).isEqualTo(1L);
        assertThat(resposta.getSintomas()).isEqualTo("Febre relatada pelo professor");
        assertThat(resposta.getContexto()).isEqualTo("Paciente adulto em atendimento ambulatorial");
        assertThat(resposta.getExamClinico()).isEqualTo("Ausculta pulmonar com sibilos");
        assertThat(resposta.getAntecClinico()).isEqualTo("Historico de asma");
        assertThat(resposta.getDiagEsperado()).isEqualTo("Exacerbacao asmatica");
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
        ConteudoClinico conteudoAtual = new ConteudoClinico();
        conteudoAtual.setIdConteudo(22L);
        conteudoAtual.setCasoClinico(caso);
        conteudoAtual.setSintomas("Dispneia e chiado");
        conteudoAtual.setContexto("Paciente procura atendimento");
        conteudoAtual.setExamClinico("Sibilos difusos");
        conteudoAtual.setAntecClinico("Asma previa");
        conteudoAtual.setDiagEsperado("Exacerbacao asmatica");

        when(casoRepository.findById(1L)).thenReturn(Optional.of(caso));
        when(pacienteRepository.findByCasoClinicoIdCasoOrderByIdPacienteAsc(1L))
                .thenReturn(List.of());
        when(conteudoRepository.findFirstByCasoClinicoIdCasoOrderByIdConteudoDesc(1L))
                .thenReturn(Optional.of(conteudoAtual));
        when(conteudoRepository.save(any(ConteudoClinico.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CasoClinicoIaResponseDTO resposta = servico.ajustarConteudo(
                1L,
                new CasoClinicoAjusteRequestDTO("SIMPLIFICAR", null, true));

        assertThat(resposta.getIdConteudo()).isEqualTo(22L);
        assertThat(resposta.getContexto()).isEqualTo("Paciente adulto em atendimento ambulatorial");
        assertThat(conteudoAtual.getExamClinico()).isEqualTo("Ausculta pulmonar com sibilos");
    }

    @Test
    void deveAtualizarPacienteEObjetivoQuandoIaComplementaInformacoes() {
        ServicoCasoClinicoIa servico = servicoComChave();
        when(aiClient.gerarConteudo(any(), any())).thenReturn(respostaIaComComplementos());

        CasoClinico caso = criarCaso();
        caso.setObjetivoAprendizagem(null);

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

        CasoClinicoIaRequestDTO requisicao = new CasoClinicoIaRequestDTO(null, null, null, null, null);
        requisicao.setDadosSinteticosOuDesidentificados(true);
        requisicao.setPermitirComplementoIa(true);
        requisicao.setInformacoesAdicionaisPaciente(
                "CPF: 123.456.789-00; outro CPF 98765432100; "
                        + "CNS 123456789012345; e-mail: maria.sigilo@example.com");

        CasoClinicoIaResponseDTO resposta = servico.gerarConteudo(1L, requisicao);

        assertThat(resposta.getIdConteudo()).isEqualTo(33L);
        assertThat(caso.getObjetivoAprendizagem()).isEqualTo("Identificar sinais de exacerbação asmática e definir conduta inicial.");
        assertThat(paciente.getNome()).isEqualTo("Maria Sigilosa");
        assertThat(paciente.getIdade()).isEqualTo(45);
        assertThat(paciente.getSexo()).isEqualTo(Sexo.MASCULINO);
        assertThat(paciente.getEstadoCivil()).isEqualTo(EstadoCivil.CASADO);
        assertThat(paciente.getProfissao()).isEqualTo("Professor");
        assertThat(paciente.getPeso()).isEqualTo("80 kg");
        assertThat(paciente.getAltura()).isEqualTo("170 cm");

        ArgumentCaptor<String> instrucoesSistema = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> contexto = ArgumentCaptor.forClass(String.class);
        verify(aiClient).gerarConteudo(instrucoesSistema.capture(), contexto.capture());
        assertThat(instrucoesSistema.getValue()).contains("dados nao confiaveis");
        assertThat(contexto.getValue())
                .contains("[DADO_REMOVIDO]")
                .doesNotContain(
                        "Maria Sigilosa",
                        "123.456.789-00",
                        "98765432100",
                        "123456789012345",
                        "maria.sigilo@example.com");
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
        resposta.setSintomas("Febre gerada pela IA");
        resposta.setContexto("Paciente adulto em atendimento ambulatorial");
        resposta.setExamClinico("Ausculta pulmonar com sibilos");
        resposta.setAntecClinico("Historico de asma");
        resposta.setDiagEsperado("Exacerbacao asmatica");
        return resposta;
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
