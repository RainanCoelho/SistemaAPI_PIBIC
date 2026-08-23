package com.SistemaApiCrud.SistemaCrud.service;

import java.util.function.Supplier;

public final class ContextoIdempotenciaGeracaoIa {

    private static final ThreadLocal<Long> ID_SOLICITACAO = new ThreadLocal<>();

    private ContextoIdempotenciaGeracaoIa() {
    }

    public static <T> T executar(Long idSolicitacao, Supplier<T> operacao) {
        ID_SOLICITACAO.set(idSolicitacao);
        try {
            return operacao.get();
        } finally {
            ID_SOLICITACAO.remove();
        }
    }

    public static Long idAtual() {
        return ID_SOLICITACAO.get();
    }
}
