package com.SistemaApiCrud.SistemaCrud.service;

/**
 * Resultado de uma chamada ao provedor de IA, sem prompt nem conteudo clinico.
 *
 * @param <T> tipo estruturado devolvido pelo provedor
 */
public record RespostaIaComMetricas<T>(
        T entidade,
        long duracaoProvedorMs,
        String modeloEfetivo,
        Integer tokensEntrada,
        Integer tokensSaida) {

    public static <T> RespostaIaComMetricas<T> semMetricas(T entidade) {
        return new RespostaIaComMetricas<>(entidade, 0L, null, null, null);
    }
}
