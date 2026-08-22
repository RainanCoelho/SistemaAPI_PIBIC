package com.SistemaApiCrud.SistemaCrud.exception;

import java.time.Instant;
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
}
