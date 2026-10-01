package com.SistemaApiCrud.SistemaCrud.dto;

import java.util.Map;

/** Contrato compacto: a revisao nao precisa gerar novamente o caso. */
public record CoerenciaIaDTO(String statusCoerencia, Map<String, String> violacoes) {

    public CoerenciaIaDTO {
        violacoes = violacoes == null ? Map.of() : Map.copyOf(violacoes);
    }

    public CasoClinicoGeradoIaDTO paraResultado() {
        CasoClinicoGeradoIaDTO resultado = new CasoClinicoGeradoIaDTO();
        resultado.setStatusCoerencia(statusCoerencia);
        resultado.setViolacoes(violacoes);
        return resultado;
    }
}
