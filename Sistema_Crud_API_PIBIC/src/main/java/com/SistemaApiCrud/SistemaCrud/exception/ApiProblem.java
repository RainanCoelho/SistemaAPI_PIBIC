package com.SistemaApiCrud.SistemaCrud.exception;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ApiProblem(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        Instant timestamp,
        String correlationId,
        String erro,
        Map<String, String> errors,
        Map<String, String> campos) {

    public ApiProblem {
        errors = copiaImutavelOrdenada(errors);
        campos = copiaImutavelOrdenada(campos);
    }

    @Override
    public Map<String, String> errors() {
        return copiaImutavelOrdenada(errors);
    }

    @Override
    public Map<String, String> campos() {
        return copiaImutavelOrdenada(campos);
    }

    private static Map<String, String> copiaImutavelOrdenada(Map<String, String> valores) {
        return valores == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(valores));
    }
}
