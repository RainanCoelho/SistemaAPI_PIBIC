package com.SistemaApiCrud.SistemaCrud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.SistemaApiCrud.SistemaCrud.DTO.AlternativaGeradaIaDTO;
import com.SistemaApiCrud.SistemaCrud.DTO.GerarPerguntasIaRequestDTO;
import com.SistemaApiCrud.SistemaCrud.DTO.PerguntaGeradaIaDTO;
import com.SistemaApiCrud.SistemaCrud.DTO.PerguntasGeradasIaDTO;
import com.SistemaApiCrud.SistemaCrud.DTO.pergunta_request_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.pergunta_response_DTO;
import com.SistemaApiCrud.SistemaCrud.entity.casos_clinicos;
import com.SistemaApiCrud.SistemaCrud.entity.conteudo_clinico;
import com.SistemaApiCrud.SistemaCrud.entity.enums.StatusCasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.enums.TipoPergunta;
import com.SistemaApiCrud.SistemaCrud.exception.AiProviderException;
import com.SistemaApiCrud.SistemaCrud.exception.ConflitoEstadoException;
import com.SistemaApiCrud.SistemaCrud.exception.RecursoNaoEncontradoException;
import com.SistemaApiCrud.SistemaCrud.exception.ServicoIndisponivelException;
import com.SistemaApiCrud.SistemaCrud.repository.caso_clinico_repository;
import com.SistemaApiCrud.SistemaCrud.repository.conteudo_clinico_repository;
import com.SistemaApiCrud.SistemaCrud.repository.paciente_repository;
import com.SistemaApiCrud.SistemaCrud.service.PerguntaAiClient;
import com.SistemaApiCrud.SistemaCrud.service.PerguntaIaService;
import com.SistemaApiCrud.SistemaCrud.service.ProtecaoDadosClinicosIa;
import com.SistemaApiCrud.SistemaCrud.service.pergunta_service;

class PerguntaIaServiceTests {

    private final PerguntaAiClient aiClient = mock(PerguntaAiClient.class);
    private final caso_clinico_repository casoRepository = mock(caso_clinico_repository.class);
    private final conteudo_clinico_repository conteudoRepository =
            mock(conteudo_clinico_repository.class);
    private final paciente_repository pacienteRepository = mock(paciente_repository.class);
    private final pergunta_service perguntaService = mock(pergunta_service.class);

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void deveGerarValidarMapearESalvarPerguntasEmLote() {
        PerguntaIaService service = serviceComChave();
        GerarPerguntasIaRequestDTO request = request(2);
        casos_clinicos caso = caso(StatusCasoClinico.RASCUNHO);
        conteudo_clinico conteudo = conteudo(caso);
        conteudo.setContexto(
                "Nome: Paciente Sigiloso; CPF: 123.456.789-00; atendimento de adulto");
        PerguntasGeradasIaDTO respostaIa = respostaValida(2);
        List<pergunta_response_DTO> respostaEsperada = List.of(new pergunta_response_DTO());

        when(casoRepository.findById(1L)).thenReturn(Optional.of(caso));
        when(conteudoRepository.findFirstByCasoClinicoIdCasoOrderByIdConteudoDesc(1L))
                .thenReturn(Optional.of(conteudo));
        when(pacienteRepository.findByCasoClinicoIdCasoOrderByIdPacienteAsc(1L))
                .thenReturn(List.of());
        when(aiClient.gerarPerguntas(any(), any())).thenReturn(respostaIa);
        when(perguntaService.salvarLoteEmCaso(anyLong(), any(), anyString(), anyLong()))
                .thenReturn(respostaEsperada);

        List<pergunta_response_DTO> resposta = service.gerarPerguntas(1L, request);

        assertThat(resposta).isSameAs(respostaEsperada);
        ArgumentCaptor<String> instrucoesSistema = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> contexto = ArgumentCaptor.forClass(String.class);
        verify(aiClient).gerarPerguntas(instrucoesSistema.capture(), contexto.capture());
        assertThat(instrucoesSistema.getValue()).contains("marcadores XML");
        assertThat(contexto.getValue())
                .contains("Gere exatamente 2 perguntas")
                .contains("diagnosticoEsperado: Pneumonia comunitaria")
                .contains("focoPedagogico: Priorize a conduta inicial")
                .contains("[DADO_REMOVIDO]")
                .doesNotContain(
                        "Paciente Sigiloso",
                        "123.456.789-00");

        ArgumentCaptor<List> perguntas = ArgumentCaptor.forClass(List.class);
        verify(perguntaService).salvarLoteEmCaso(
                org.mockito.ArgumentMatchers.eq(1L),
                perguntas.capture(),
                anyString(),
                org.mockito.ArgumentMatchers.eq(0L));
        List<pergunta_request_DTO> perguntasMapeadas = perguntas.getValue();
        assertThat(perguntasMapeadas).hasSize(2);
        assertThat(perguntasMapeadas.get(0).getTipo()).isEqualTo(TipoPergunta.MULTIPLA_ESCOLHA);
        assertThat(perguntasMapeadas.get(0).getAlternativas())
                .hasSize(4)
                .filteredOn(alternativa -> Boolean.TRUE.equals(alternativa.getCorreta()))
                .singleElement()
                .extracting(alternativa -> alternativa.getLetra())
                .isEqualTo("A");
    }

    @Test
    void deveExigirChaveAntesDeAcessarOProvedor() {
        PerguntaIaService service = new PerguntaIaService(
                aiClient,
                casoRepository,
                conteudoRepository,
                pacienteRepository,
                perguntaService,
                new ProtecaoDadosClinicosIa(),
                "");

        assertThatThrownBy(() -> service.gerarPerguntas(1L, request(1)))
                .isInstanceOf(ServicoIndisponivelException.class)
                .hasMessage("Configure a variavel IA_CHAVE_API antes de gerar perguntas com IA");

        verifyNoInteractions(aiClient, casoRepository, conteudoRepository, pacienteRepository, perguntaService);
    }

    @Test
    void deveExigirConteudoClinicoMesmoParaCasoManual() {
        PerguntaIaService service = serviceComChave();
        when(casoRepository.findById(1L))
                .thenReturn(Optional.of(caso(StatusCasoClinico.RASCUNHO)));
        when(conteudoRepository.findFirstByCasoClinicoIdCasoOrderByIdConteudoDesc(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.gerarPerguntas(1L, request(1)))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Conteudo clinico nao encontrado para este caso");

        verifyNoInteractions(aiClient, perguntaService);
    }

    @Test
    void naoDeveGerarPerguntasParaCasoPublicado() {
        PerguntaIaService service = serviceComChave();
        when(casoRepository.findById(1L))
                .thenReturn(Optional.of(caso(StatusCasoClinico.PUBLICADO)));

        assertThatThrownBy(() -> service.gerarPerguntas(1L, request(1)))
                .isInstanceOf(ConflitoEstadoException.class)
                .hasMessageContaining("rascunho");

        verifyNoInteractions(aiClient, conteudoRepository, pacienteRepository, perguntaService);
    }

    @Test
    void naoDeveSalvarQuandoIaRetornaQuantidadeDiferente() {
        PerguntaIaService service = prepararCasoValido();
        when(aiClient.gerarPerguntas(any(), any())).thenReturn(respostaValida(1));

        assertThatThrownBy(() -> service.gerarPerguntas(1L, request(2)))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("A IA retornou uma quantidade de perguntas diferente da solicitada");

        verify(perguntaService, never()).salvarLoteEmCaso(
                anyLong(), any(), anyString(), anyLong());
    }

    @Test
    void naoDeveSalvarQuandoGabaritoNaoCorrespondeAAlternativaCorreta() {
        PerguntaIaService service = prepararCasoValido();
        PerguntaGeradaIaDTO pergunta = perguntaValida("1");
        pergunta.setGabarito("B");
        when(aiClient.gerarPerguntas(any(), any()))
                .thenReturn(new PerguntasGeradasIaDTO(List.of(pergunta)));

        assertThatThrownBy(() -> service.gerarPerguntas(1L, request(1)))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("O gabarito retornado pela IA nao corresponde a alternativa correta");

        verify(perguntaService, never()).salvarLoteEmCaso(
                anyLong(), any(), anyString(), anyLong());
    }

    @Test
    void naoDeveSalvarPerguntasDuplicadas() {
        PerguntaIaService service = prepararCasoValido();
        PerguntaGeradaIaDTO primeira = perguntaValida("1");
        PerguntaGeradaIaDTO segunda = perguntaValida("2");
        segunda.setTexto("  " + primeira.getTexto().toUpperCase() + "  ");
        when(aiClient.gerarPerguntas(any(), any()))
                .thenReturn(new PerguntasGeradasIaDTO(List.of(primeira, segunda)));

        assertThatThrownBy(() -> service.gerarPerguntas(1L, request(2)))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("A IA retornou perguntas duplicadas");

        verify(perguntaService, never()).salvarLoteEmCaso(
                anyLong(), any(), anyString(), anyLong());
    }

    @Test
    void naoDeveSalvarQuandoIaRetornaLetrasForaDaSequenciaEsperada() {
        PerguntaIaService service = prepararCasoValido();
        PerguntaGeradaIaDTO pergunta = perguntaValida("1");
        pergunta.getAlternativas().get(3).setLetra("X");
        when(aiClient.gerarPerguntas(any(), any()))
                .thenReturn(new PerguntasGeradasIaDTO(List.of(pergunta)));

        assertThatThrownBy(() -> service.gerarPerguntas(1L, request(1)))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("A IA deve identificar as alternativas com letras sequenciais de A ate D");

        verify(perguntaService, never()).salvarLoteEmCaso(
                anyLong(), any(), anyString(), anyLong());
    }

    @Test
    void naoDeveSalvarQuandoIaRetornaTextosDeAlternativasDuplicados() {
        PerguntaIaService servico = prepararCasoValido();
        PerguntaGeradaIaDTO pergunta = perguntaValida("1");
        String textoDuplicado = pergunta.getAlternativas().get(0).getTexto();
        pergunta.getAlternativas().get(1).setTexto("  " + textoDuplicado.toUpperCase() + "  ");
        when(aiClient.gerarPerguntas(any(), any()))
                .thenReturn(new PerguntasGeradasIaDTO(List.of(pergunta)));

        assertThatThrownBy(() -> servico.gerarPerguntas(1L, request(1)))
                .isInstanceOf(AiProviderException.class)
                .hasMessage("A IA retornou textos de alternativas duplicados");

        verify(perguntaService, never()).salvarLoteEmCaso(
                anyLong(), any(), anyString(), anyLong());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void deveGerarPerguntaDiscursivaSemAlternativas() {
        PerguntaIaService service = prepararCasoValido();
        GerarPerguntasIaRequestDTO request = new GerarPerguntasIaRequestDTO(
                1,
                TipoPergunta.DISCURSIVA,
                null,
                null);
        PerguntaGeradaIaDTO perguntaGerada = new PerguntaGeradaIaDTO(
                "Explique o raciocinio diagnostico.",
                "Rubrica: integrar achados clinicos, hipotese e justificativa.",
                "REVISAO_MANUAL",
                List.of());
        when(aiClient.gerarPerguntas(any(), any()))
                .thenReturn(new PerguntasGeradasIaDTO(List.of(perguntaGerada)));
        when(perguntaService.salvarLoteEmCaso(anyLong(), any(), anyString(), anyLong()))
                .thenReturn(List.of(new pergunta_response_DTO()));

        service.gerarPerguntas(1L, request);

        ArgumentCaptor<List> perguntas = ArgumentCaptor.forClass(List.class);
        verify(perguntaService).salvarLoteEmCaso(
                org.mockito.ArgumentMatchers.eq(1L),
                perguntas.capture(),
                anyString(),
                org.mockito.ArgumentMatchers.eq(0L));
        pergunta_request_DTO pergunta = (pergunta_request_DTO) perguntas.getValue().get(0);
        assertThat(pergunta.getTipo()).isEqualTo(TipoPergunta.DISCURSIVA);
        assertThat(pergunta.getAlternativas()).isEmpty();
    }

    @Test
    void naoDeveSalvarVerdadeiroOuFalsoComGabaritoForaDoContrato() {
        PerguntaIaService service = prepararCasoValido();
        GerarPerguntasIaRequestDTO requisicao = new GerarPerguntasIaRequestDTO(
                1,
                TipoPergunta.VERDADEIRO_FALSO,
                null,
                null);
        PerguntaGeradaIaDTO perguntaGerada = new PerguntaGeradaIaDTO(
                "Antibiotico e sempre indicado neste quadro.",
                "A indicacao depende da etiologia e da avaliacao clinica.",
                "DEPENDE",
                List.of());
        when(aiClient.gerarPerguntas(any(), any()))
                .thenReturn(new PerguntasGeradasIaDTO(List.of(perguntaGerada)));

        assertThatThrownBy(() -> service.gerarPerguntas(1L, requisicao))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("VERDADEIRO ou FALSO");

        verify(perguntaService, never()).salvarLoteEmCaso(
                anyLong(), any(), anyString(), anyLong());
    }

    @Test
    void deveAceitarDiagnosticoComSinonimosSeparados() {
        PerguntaIaService service = prepararCasoValido();
        GerarPerguntasIaRequestDTO requisicao = new GerarPerguntasIaRequestDTO(
                1,
                TipoPergunta.DIAGNOSTICO,
                null,
                null);
        PerguntaGeradaIaDTO perguntaGerada = new PerguntaGeradaIaDTO(
                "Qual e o diagnostico mais provavel?",
                "Os achados sustentam infeccao pulmonar adquirida na comunidade.",
                "Pneumonia adquirida na comunidade|Pneumonia comunitaria",
                List.of());
        when(aiClient.gerarPerguntas(any(), any()))
                .thenReturn(new PerguntasGeradasIaDTO(List.of(perguntaGerada)));
        when(perguntaService.salvarLoteEmCaso(anyLong(), any(), anyString(), anyLong()))
                .thenReturn(List.of(new pergunta_response_DTO()));

        service.gerarPerguntas(1L, requisicao);

        verify(perguntaService).salvarLoteEmCaso(
                anyLong(), any(), anyString(), anyLong());
    }

    private PerguntaIaService prepararCasoValido() {
        PerguntaIaService service = serviceComChave();
        casos_clinicos caso = caso(StatusCasoClinico.RASCUNHO);
        when(casoRepository.findById(1L)).thenReturn(Optional.of(caso));
        when(conteudoRepository.findFirstByCasoClinicoIdCasoOrderByIdConteudoDesc(1L))
                .thenReturn(Optional.of(conteudo(caso)));
        when(pacienteRepository.findByCasoClinicoIdCasoOrderByIdPacienteAsc(1L))
                .thenReturn(List.of());
        return service;
    }

    private PerguntaIaService serviceComChave() {
        return new PerguntaIaService(
                aiClient,
                casoRepository,
                conteudoRepository,
                pacienteRepository,
                perguntaService,
                new ProtecaoDadosClinicosIa(),
                "chave-teste");
    }

    private GerarPerguntasIaRequestDTO request(int quantidade) {
        return new GerarPerguntasIaRequestDTO(
                quantidade,
                TipoPergunta.MULTIPLA_ESCOLHA,
                4,
                "Priorize a conduta inicial");
    }

    private PerguntasGeradasIaDTO respostaValida(int quantidade) {
        java.util.ArrayList<PerguntaGeradaIaDTO> perguntas = new java.util.ArrayList<>();
        for (int indice = 1; indice <= quantidade; indice++) {
            perguntas.add(perguntaValida(String.valueOf(indice)));
        }
        return new PerguntasGeradasIaDTO(perguntas);
    }

    private PerguntaGeradaIaDTO perguntaValida(String sufixo) {
        return new PerguntaGeradaIaDTO(
                "Qual e a melhor conduta " + sufixo + "?",
                "A conduta A e a mais indicada.",
                "A",
                List.of(
                        new AlternativaGeradaIaDTO("A", "Iniciar tratamento adequado", true),
                        new AlternativaGeradaIaDTO("B", "Apenas observar", false),
                        new AlternativaGeradaIaDTO("C", "Suspender todo tratamento", false),
                        new AlternativaGeradaIaDTO("D", "Ignorar os sinais de gravidade", false)));
    }

    private casos_clinicos caso(StatusCasoClinico status) {
        casos_clinicos caso = new casos_clinicos();
        caso.setIdCaso(1L);
        caso.setStatus(status);
        caso.setTitulo("Caso respiratorio");
        caso.setDisciplina("Clinica Medica");
        caso.setAreaSaude("Medicina");
        caso.setEspecialidade("Pneumologia");
        caso.setDificuldade("MEDIA");
        caso.setEstilo("Raciocinio clinico");
        caso.setObjetivoAprendizagem("Definir diagnostico e conduta");
        return caso;
    }

    private conteudo_clinico conteudo(casos_clinicos caso) {
        conteudo_clinico conteudo = new conteudo_clinico();
        conteudo.setIdConteudo(10L);
        conteudo.setCasoClinico(caso);
        conteudo.setSintomas("Febre, tosse e dispneia");
        conteudo.setContexto("Atendimento de adulto na emergencia");
        conteudo.setExamClinico("Crepitacoes em base pulmonar direita");
        conteudo.setAntecClinico("Sem comorbidades");
        conteudo.setDiagEsperado("Pneumonia comunitaria");
        return conteudo;
    }
}
