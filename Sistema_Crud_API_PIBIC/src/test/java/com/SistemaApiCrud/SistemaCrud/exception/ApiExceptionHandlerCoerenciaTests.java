package com.SistemaApiCrud.SistemaCrud.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

class ApiExceptionHandlerCoerenciaTests {

    @Test
    void deveRetornar422ComCamposClinicosIncoerentes() {
        ApiExceptionHandler handler = new ApiExceptionHandler(new ApiProblemSupport());
        MockHttpServletRequest requisicao = new MockHttpServletRequest(
                "POST",
                "/casos/41/ia/gerar");
        CoerenciaCasoClinicoException excecao = new CoerenciaCasoClinicoException(
                "Os dados informados sao clinicamente incoerentes",
                Map.of(
                        "especialidade", "A especialidade conflita com o diagnostico",
                        "diagEsperado", "O diagnostico pertence a outro contexto"));

        var resposta = handler.tratarCoerenciaCasoClinico(excecao, requisicao);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().type())
                .isEqualTo("urn:sistema-api-pibic:problem:dados-clinicos-incoerentes");
        assertThat(resposta.getBody().campos())
                .containsKeys("especialidade", "diagEsperado");
    }
}
