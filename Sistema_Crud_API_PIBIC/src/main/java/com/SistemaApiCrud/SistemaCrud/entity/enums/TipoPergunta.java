package com.SistemaApiCrud.SistemaCrud.entity.enums;

import java.util.Set;

public enum TipoPergunta {
    MULTIPLA_ESCOLHA,
    DISCURSIVA,
    VERDADEIRO_FALSO,
    DIAGNOSTICO,
    CONDUTA_CLINICA;

    private static final Set<TipoPergunta> TIPOS_DISPONIVEIS = Set.of(
            MULTIPLA_ESCOLHA,
            DISCURSIVA,
            VERDADEIRO_FALSO);

    public boolean disponivelParaNovasPerguntas() {
        return TIPOS_DISPONIVEIS.contains(this);
    }

    public static int quantidadeTiposDisponiveis() {
        return TIPOS_DISPONIVEIS.size();
    }
}
