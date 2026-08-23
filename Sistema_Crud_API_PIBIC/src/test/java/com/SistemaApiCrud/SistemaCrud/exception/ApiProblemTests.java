package com.SistemaApiCrud.SistemaCrud.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ApiProblemTests {

    @Test
    void deveManterCopiasImutaveisDosMapasRecebidos() {
        Map<String, String> erros = new LinkedHashMap<>();
        erros.put("primeiro", "invalido");
        erros.put("segundo", "ausente");

        ApiProblem problema = novoProblema(erros, erros);
        erros.put("outro", "erro");

        assertThat(problema.errors()).containsExactly(
                Map.entry("primeiro", "invalido"),
                Map.entry("segundo", "ausente"));
        assertThat(problema.campos()).containsExactly(
                Map.entry("primeiro", "invalido"),
                Map.entry("segundo", "ausente"));
        assertThatThrownBy(() -> problema.errors().put("novo", "erro"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void deveNormalizarMapasNulosParaMapasVazios() {
        ApiProblem problema = novoProblema(null, null);

        assertThat(problema.errors()).isEmpty();
        assertThat(problema.campos()).isEmpty();
    }

    private ApiProblem novoProblema(
            Map<String, String> errors,
            Map<String, String> campos) {
        return new ApiProblem(
                "urn:teste",
                "Erro de teste",
                400,
                "Detalhe",
                "/teste",
                Instant.EPOCH,
                "correlation-id",
                "Erro",
                errors,
                campos);
    }
}
