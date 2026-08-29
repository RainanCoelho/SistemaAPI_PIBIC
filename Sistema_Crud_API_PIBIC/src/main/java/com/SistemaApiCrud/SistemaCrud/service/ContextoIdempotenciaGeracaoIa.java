package com.SistemaApiCrud.SistemaCrud.service;

import java.util.function.Supplier;

public final class ContextoIdempotenciaGeracaoIa {

    private static final ThreadLocal<Contexto> CONTEXTO = new ThreadLocal<>();

    private ContextoIdempotenciaGeracaoIa() {
    }

    public static <T> T executar(Long idSolicitacao, Supplier<T> operacao) {
        Contexto anterior = CONTEXTO.get();
        CONTEXTO.set(new Contexto(idSolicitacao));
        try {
            return operacao.get();
        } finally {
            if (anterior == null) {
                CONTEXTO.remove();
            } else {
                CONTEXTO.set(anterior);
            }
        }
    }

    public static Long idAtual() {
        Contexto contexto = CONTEXTO.get();
        return contexto == null ? null : contexto.idSolicitacao;
    }

    static boolean deveRegistrarUso() {
        Contexto contexto = CONTEXTO.get();
        return contexto == null || !contexto.usoRegistrado;
    }

    static void marcarUsoRegistrado() {
        Contexto contexto = CONTEXTO.get();
        if (contexto != null) {
            contexto.usoRegistrado = true;
        }
    }

    private static final class Contexto {

        private final Long idSolicitacao;
        private boolean usoRegistrado;

        private Contexto(Long idSolicitacao) {
            this.idSolicitacao = idSolicitacao;
        }
    }
}
