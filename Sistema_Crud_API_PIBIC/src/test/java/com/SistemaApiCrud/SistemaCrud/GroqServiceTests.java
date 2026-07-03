package com.SistemaApiCrud.SistemaCrud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.SistemaApiCrud.SistemaCrud.DTO.CasoClinicoAjusteRequestDTO;
import com.SistemaApiCrud.SistemaCrud.DTO.CasoClinicoRequestDTO;
import com.SistemaApiCrud.SistemaCrud.DTO.CasoClinicoResponseDTO;
import com.SistemaApiCrud.SistemaCrud.entity.Professor;
import com.SistemaApiCrud.SistemaCrud.entity.casos_clinicos;
import com.SistemaApiCrud.SistemaCrud.entity.conteudo_clinico;
import com.SistemaApiCrud.SistemaCrud.entity.paciente;
import com.SistemaApiCrud.SistemaCrud.entity.enums.EstadoCivil;
import com.SistemaApiCrud.SistemaCrud.entity.enums.Sexo;
import com.SistemaApiCrud.SistemaCrud.exception.BusinessException;
import com.SistemaApiCrud.SistemaCrud.repository.caso_clinico_repository;
import com.SistemaApiCrud.SistemaCrud.repository.conteudo_clinico_repository;
import com.SistemaApiCrud.SistemaCrud.repository.paciente_repository;
import com.SistemaApiCrud.SistemaCrud.service.GroqService;
import com.google.gson.Gson;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;

class GroqServiceTests {

    private final caso_clinico_repository casoRepository = mock(caso_clinico_repository.class);
    private final conteudo_clinico_repository conteudoRepository = mock(conteudo_clinico_repository.class);
    private final paciente_repository pacienteRepository = mock(paciente_repository.class);

    @Test
    void deveCompletarCamposVaziosComRespostaDaGroqEPreservarDadosDoProfessor() {
        GroqService service = new GroqService(
                clientComRespostaGroq(),
                new Gson(),
                casoRepository,
                conteudoRepository,
                pacienteRepository,
                "chave-teste",
                "llama-3.3-70b-versatile");

        casos_clinicos caso = criarCaso();
        when(casoRepository.findById(1L)).thenReturn(Optional.of(caso));
        when(pacienteRepository.findByCasoClinicoIdCaso(1L)).thenReturn(List.of());
        when(conteudoRepository.save(any(conteudo_clinico.class))).thenAnswer(invocation -> {
            conteudo_clinico conteudo = invocation.getArgument(0);
            conteudo.setIdConteudo(10L);
            return conteudo;
        });

        CasoClinicoRequestDTO request = new CasoClinicoRequestDTO(
                "Febre relatada pelo professor",
                null,
                null,
                null,
                null);

        CasoClinicoResponseDTO response = service.gerarConteudo(1L, request);

        assertThat(response.getIdConteudo()).isEqualTo(10L);
        assertThat(response.getIdCaso()).isEqualTo(1L);
        assertThat(response.getSintomas()).isEqualTo("Febre relatada pelo professor");
        assertThat(response.getContexto()).isEqualTo("Paciente adulto em atendimento ambulatorial");
        assertThat(response.getExamClinico()).isEqualTo("Ausculta pulmonar com sibilos");
        assertThat(response.getAntecClinico()).isEqualTo("Historico de asma");
        assertThat(response.getDiagEsperado()).isEqualTo("Exacerbacao asmatica");
    }

    @Test
    void deveExigirChaveGroqQuandoExistemCamposParaGerar() {
        GroqService service = new GroqService(
                new OkHttpClient(),
                new Gson(),
                casoRepository,
                conteudoRepository,
                pacienteRepository,
                "",
                "llama-3.3-70b-versatile");

        when(casoRepository.findById(1L)).thenReturn(Optional.of(criarCaso()));

        CasoClinicoRequestDTO request = new CasoClinicoRequestDTO(
                "Sintoma informado",
                null,
                "Exame informado",
                "Antecedente informado",
                "Diagnostico informado");

        assertThatThrownBy(() -> service.gerarConteudo(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Configure a variavel GROQ_API_KEY antes de gerar conteudo com IA");
    }

    @Test
    void deveAjustarConteudoClinicoExistenteComIa() {
        GroqService service = new GroqService(
                clientComRespostaGroq(),
                new Gson(),
                casoRepository,
                conteudoRepository,
                pacienteRepository,
                "chave-teste",
                "llama-3.3-70b-versatile");

        casos_clinicos caso = criarCaso();
        conteudo_clinico conteudoAtual = new conteudo_clinico();
        conteudoAtual.setIdConteudo(22L);
        conteudoAtual.setCasoClinico(caso);
        conteudoAtual.setSintomas("Dispneia e chiado");
        conteudoAtual.setContexto("Paciente procura atendimento");
        conteudoAtual.setExamClinico("Sibilos difusos");
        conteudoAtual.setAntecClinico("Asma previa");
        conteudoAtual.setDiagEsperado("Exacerbacao asmatica");

        when(casoRepository.findById(1L)).thenReturn(Optional.of(caso));
        when(pacienteRepository.findByCasoClinicoIdCaso(1L)).thenReturn(List.of());
        when(conteudoRepository.findFirstByCasoClinicoIdCasoOrderByIdConteudoDesc(1L))
                .thenReturn(Optional.of(conteudoAtual));
        when(conteudoRepository.save(any(conteudo_clinico.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CasoClinicoResponseDTO response = service.ajustarConteudo(
                1L,
                new CasoClinicoAjusteRequestDTO("SIMPLIFICAR", null));

        assertThat(response.getIdConteudo()).isEqualTo(22L);
        assertThat(response.getContexto()).isEqualTo("Paciente adulto em atendimento ambulatorial");
        assertThat(conteudoAtual.getExamClinico()).isEqualTo("Ausculta pulmonar com sibilos");
    }

    @Test
    void deveAtualizarPacienteEObjetivoQuandoIaComplementaInformacoes() {
        GroqService service = new GroqService(
                clientComRespostaGroqComComplementos(),
                new Gson(),
                casoRepository,
                conteudoRepository,
                pacienteRepository,
                "chave-teste",
                "llama-3.3-70b-versatile");

        casos_clinicos caso = criarCaso();
        caso.setObjetivoAprendizagem(null);

        paciente paciente = new paciente();
        paciente.setIdPaciente(5L);
        paciente.setCasoClinico(caso);
        paciente.setNome("NAO_INFORMADO");
        paciente.setIdade(0);
        paciente.setSexo(Sexo.NAO_INFORMADO);
        paciente.setEstadoCivil(EstadoCivil.NAO_INFORMADO);
        paciente.setProfissao("NAO_INFORMADO");
        paciente.setPeso("NAO_INFORMADO");
        paciente.setAltura("NAO_INFORMADO");

        when(casoRepository.findById(1L)).thenReturn(Optional.of(caso));
        when(casoRepository.save(any(casos_clinicos.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pacienteRepository.findByCasoClinicoIdCaso(1L)).thenReturn(List.of(paciente));
        when(pacienteRepository.save(any(paciente.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conteudoRepository.save(any(conteudo_clinico.class))).thenAnswer(invocation -> {
            conteudo_clinico conteudo = invocation.getArgument(0);
            conteudo.setIdConteudo(33L);
            return conteudo;
        });

        CasoClinicoRequestDTO request = new CasoClinicoRequestDTO(null, null, null, null, null);
        request.setPermitirComplementoIa(true);

        CasoClinicoResponseDTO response = service.gerarConteudo(1L, request);

        assertThat(response.getIdConteudo()).isEqualTo(33L);
        assertThat(caso.getObjetivoAprendizagem()).isEqualTo("Identificar sinais de exacerbação asmática e definir conduta inicial.");
        assertThat(paciente.getNome()).isEqualTo("Carlos Henrique");
        assertThat(paciente.getIdade()).isEqualTo(45);
        assertThat(paciente.getSexo()).isEqualTo(Sexo.MASCULINO);
        assertThat(paciente.getEstadoCivil()).isEqualTo(EstadoCivil.CASADO);
        assertThat(paciente.getProfissao()).isEqualTo("Professor");
        assertThat(paciente.getPeso()).isEqualTo("80 kg");
        assertThat(paciente.getAltura()).isEqualTo("170 cm");
    }

    @Test
    void deveAtualizarPacienteQuandoAjusteSolicitaMudancaNosDados() {
        GroqService service = new GroqService(
                clientComRespostaGroqAjustandoPaciente(),
                new Gson(),
                casoRepository,
                conteudoRepository,
                pacienteRepository,
                "chave-teste",
                "llama-3.3-70b-versatile");

        casos_clinicos caso = criarCaso();
        conteudo_clinico conteudoAtual = new conteudo_clinico();
        conteudoAtual.setIdConteudo(44L);
        conteudoAtual.setCasoClinico(caso);
        conteudoAtual.setSintomas("Dor no peito e falta de ar");
        conteudoAtual.setContexto("Paciente de 55 anos com quadro cardiologico");
        conteudoAtual.setExamClinico("ECG com alteracoes isquemicas");
        conteudoAtual.setAntecClinico("Hipertensao");
        conteudoAtual.setDiagEsperado("Angina instavel");

        paciente paciente = new paciente();
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
        when(casoRepository.save(any(casos_clinicos.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pacienteRepository.findByCasoClinicoIdCaso(1L)).thenReturn(List.of(paciente));
        when(pacienteRepository.save(any(paciente.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(conteudoRepository.findFirstByCasoClinicoIdCasoOrderByIdConteudoDesc(1L))
                .thenReturn(Optional.of(conteudoAtual));
        when(conteudoRepository.save(any(conteudo_clinico.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CasoClinicoResponseDTO response = service.ajustarConteudo(
                1L,
                new CasoClinicoAjusteRequestDTO("PERSONALIZADO", "Paciente precisa ter menos que 25 anos"));

        assertThat(response.getIdConteudo()).isEqualTo(44L);
        assertThat(paciente.getIdade()).isEqualTo(22);
        assertThat(conteudoAtual.getContexto()).contains("22 anos");
    }

    private OkHttpClient clientComRespostaGroq() {
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> new Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(ResponseBody.create(respostaGroq(), MediaType.get("application/json")))
                        .build())
                .build();
    }

    private String respostaGroq() {
        return """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"sintomas\\":\\"Febre gerada pela IA\\",\\"contexto\\":\\"Paciente adulto em atendimento ambulatorial\\",\\"examClinico\\":\\"Ausculta pulmonar com sibilos\\",\\"antecClinico\\":\\"Historico de asma\\",\\"diagEsperado\\":\\"Exacerbacao asmatica\\"}"
                      }
                    }
                  ]
                }
                """;
    }

    private OkHttpClient clientComRespostaGroqComComplementos() {
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> new Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(ResponseBody.create(respostaGroqComComplementos(), MediaType.get("application/json")))
                        .build())
                .build();
    }

    private String respostaGroqComComplementos() {
        return """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"sintomas\\":\\"Dispneia e chiado\\",\\"contexto\\":\\"Paciente procura atendimento por piora respiratoria\\",\\"examClinico\\":\\"Sibilos difusos\\",\\"antecClinico\\":\\"Asma previa\\",\\"diagEsperado\\":\\"Exacerbacao asmatica\\",\\"objetivoAprendizagem\\":\\"Identificar sinais de exacerbação asmática e definir conduta inicial.\\",\\"paciente\\":{\\"nome\\":\\"Carlos Henrique\\",\\"idade\\":45,\\"sexo\\":\\"MASCULINO\\",\\"estadoCivil\\":\\"CASADO\\",\\"profissao\\":\\"Professor\\",\\"peso\\":\\"80 kg\\",\\"altura\\":\\"170 cm\\"}}"
                      }
                    }
                  ]
                }
                """;
    }

    private OkHttpClient clientComRespostaGroqAjustandoPaciente() {
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> new Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(ResponseBody.create(respostaGroqAjustandoPaciente(), MediaType.get("application/json")))
                        .build())
                .build();
    }

    private String respostaGroqAjustandoPaciente() {
        return """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"sintomas\\":\\"Dor no peito intermitente e falta de ar aos esforços\\",\\"contexto\\":\\"Paciente João Silva, 22 anos, sexo masculino, casado, engenheiro, com peso de 80kg e altura de 1.75m.\\",\\"examClinico\\":\\"ECG com alteracoes isquemicas\\",\\"antecClinico\\":\\"Hipertensao\\",\\"diagEsperado\\":\\"Angina instavel\\",\\"objetivoAprendizagem\\":\\"Avaliar conduta respiratoria\\",\\"paciente\\":{\\"nome\\":\\"João Silva\\",\\"idade\\":22,\\"sexo\\":\\"MASCULINO\\",\\"estadoCivil\\":\\"CASADO\\",\\"profissao\\":\\"Engenheiro\\",\\"peso\\":\\"80kg\\",\\"altura\\":\\"1.75m\\"}}"
                      }
                    }
                  ]
                }
                """;
    }

    private casos_clinicos criarCaso() {
        casos_clinicos caso = new casos_clinicos();
        caso.setIdCaso(1L);
        caso.setProfessor(new Professor(1L, "Dra. Ana", "ana@email.com", "Clinica"));
        caso.setTitulo("Caso respiratorio");
        caso.setDificuldade("MEDIA");
        caso.setDisciplina("Clinica Medica");
        caso.setAreaSaude("Medicina");
        caso.setEstilo("Caso guiado");
        caso.setEspecialidade("Pneumologia");
        caso.setObjetivoAprendizagem("Avaliar conduta respiratoria");
        return caso;
    }
}
