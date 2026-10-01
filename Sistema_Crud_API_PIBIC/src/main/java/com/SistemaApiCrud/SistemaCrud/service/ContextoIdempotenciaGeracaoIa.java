package com.SistemaApiCrud.SistemaCrud.service;

import java.util.function.Supplier;

public final class ContextoIdempotenciaGeracaoIa {

    private static final ThreadLocal<Contexto> CONTEXTO = new ThreadLocal<>();

    private ContextoIdempotenciaGeracaoIa() {
    }

    public static <T> T executar(Long idSolicitacao, Supplier<T> operacao) {
        return executar(idSolicitacao, java.time.Duration.ofMinutes(6), operacao);
    }

    public static <T> T executar(Long idSolicitacao, java.time.Duration prazo, Supplier<T> operacao) {
        Contexto anterior = CONTEXTO.get();
        CONTEXTO.set(new Contexto(idSolicitacao, prazo));
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

    public static void exigirTempoDisponivel(java.time.Duration necessario) {
        Contexto contexto = CONTEXTO.get();
        if (contexto != null && contexto.prazoNanos - System.nanoTime() < necessario.toNanos()) {
            throw new com.SistemaApiCrud.SistemaCrud.exception.TempoEsgotadoIaException(
                    "O orcamento total da operacao de IA foi atingido; tente novamente", null);
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
        private final long prazoNanos;

        private Contexto(Long idSolicitacao, java.time.Duration prazo) {
            this.idSolicitacao = idSolicitacao;
            this.prazoNanos = System.nanoTime() + prazo.toNanos();
        }
    }
}
