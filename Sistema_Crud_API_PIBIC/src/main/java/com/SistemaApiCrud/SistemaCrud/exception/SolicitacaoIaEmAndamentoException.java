package com.SistemaApiCrud.SistemaCrud.exception;

public class SolicitacaoIaEmAndamentoException extends RuntimeException {

    private final long retryAfterSeconds;

    public SolicitacaoIaEmAndamentoException(long retryAfterSeconds) {
        super("Uma solicitacao identica de IA ainda esta em andamento. Tente novamente em breve.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
