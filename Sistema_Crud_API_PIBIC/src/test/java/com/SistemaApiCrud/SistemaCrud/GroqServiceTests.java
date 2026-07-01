package com.SistemaApiCrud.SistemaCrud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.SistemaApiCrud.SistemaCrud.DTO.CasoClinicoRequestDTO;
import com.SistemaApiCrud.SistemaCrud.DTO.CasoClinicoResponseDTO;
import com.SistemaApiCrud.SistemaCrud.entity.Professor;
import com.SistemaApiCrud.SistemaCrud.entity.casos_clinicos;
import com.SistemaApiCrud.SistemaCrud.entity.conteudo_clinico;
import com.SistemaApiCrud.SistemaCrud.exception.BusinessException;
import com.SistemaApiCrud.SistemaCrud.repository.caso_clinico_repository;
import com.SistemaApiCrud.SistemaCrud.repository.conteudo_clinico_repository;
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

    @Test
    void deveCompletarCamposVaziosComRespostaDaGroqEPreservarDadosDoProfessor() {
        GroqService service = new GroqService(
                clientComRespostaGroq(),
                new Gson(),
                casoRepository,
                conteudoRepository,
                "chave-teste",
                "llama-3.3-70b-versatile");

        casos_clinicos caso = criarCaso();
        when(casoRepository.findById(1L)).thenReturn(Optional.of(caso));
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
