package com.SistemaApiCrud.SistemaCrud.exception;

public class ConflitoIdempotenciaException extends RuntimeException {

    public ConflitoIdempotenciaException() {
        super("A chave de idempotencia ja foi usada com outra requisicao ou falhou. Envie uma nova chave.");
    }
}
