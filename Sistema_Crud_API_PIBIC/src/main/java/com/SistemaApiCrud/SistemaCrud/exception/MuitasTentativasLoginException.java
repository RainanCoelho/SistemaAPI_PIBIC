package com.SistemaApiCrud.SistemaCrud.exception;

public class MuitasTentativasLoginException extends RuntimeException {

    private final long retryAfterSeconds;

    public MuitasTentativasLoginException(long retryAfterSeconds) {
        super("Muitas tentativas de login. Tente novamente mais tarde");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
