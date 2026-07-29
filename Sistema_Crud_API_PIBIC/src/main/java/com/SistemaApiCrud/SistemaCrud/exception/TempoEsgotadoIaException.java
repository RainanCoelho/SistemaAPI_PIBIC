package com.SistemaApiCrud.SistemaCrud.exception;

public class TempoEsgotadoIaException extends RuntimeException {

    public TempoEsgotadoIaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
