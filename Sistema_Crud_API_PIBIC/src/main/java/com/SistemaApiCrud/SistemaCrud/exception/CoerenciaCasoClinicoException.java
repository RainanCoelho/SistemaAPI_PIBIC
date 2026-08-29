package com.SistemaApiCrud.SistemaCrud.exception;

import java.util.LinkedHashMap;
import java.util.Map;

public class CoerenciaCasoClinicoException extends RuntimeException {

    private final Map<String, String> campos;

    public CoerenciaCasoClinicoException(String message, Map<String, String> campos) {
        super(message);
        this.campos = Map.copyOf(new LinkedHashMap<>(campos));
    }

    public Map<String, String> getCampos() {
        return campos;
    }
}
