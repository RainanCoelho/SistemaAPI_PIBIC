package com.SistemaApiCrud.SistemaCrud.exception;

public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
