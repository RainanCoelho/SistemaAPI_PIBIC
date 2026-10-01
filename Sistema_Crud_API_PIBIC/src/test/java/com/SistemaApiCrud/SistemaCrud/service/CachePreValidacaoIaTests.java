package com.SistemaApiCrud.SistemaCrud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class CachePreValidacaoIaTests {

    @Test
    void expiraSemReterContextosAntigosENaoMisturaVersoes() {
        CachePreValidacaoIa cache = new CachePreValidacaoIa();
        cache.aprovar("v1|modelo|dados", Duration.ofNanos(1));
        assertThat(cache.aprovado("v1|modelo|dados")).isFalse();
        cache.aprovar("v1|modelo|dados", Duration.ofMinutes(1));
        assertThat(cache.aprovado("v1|modelo|dados")).isTrue();
        assertThat(cache.aprovado("v2|modelo|dados")).isFalse();
        for (int i = 0; i < 256; i++) {
            cache.aprovar("contexto" + i, Duration.ofMinutes(1));
        }
        assertThat(cache.aprovado("v1|modelo|dados")).isFalse();
    }

    @Test
    void naoIniciaOutraChamadaSemOrcamentoELimpaContextoAoFalhar() {
        assertThatThrownBy(() -> ContextoIdempotenciaGeracaoIa.executar(1L, Duration.ZERO, () -> {
            ContextoIdempotenciaGeracaoIa.exigirTempoDisponivel(Duration.ofSeconds(60));
            return "nao deve executar";
        })).isInstanceOf(com.SistemaApiCrud.SistemaCrud.exception.TempoEsgotadoIaException.class);
        assertThat(ContextoIdempotenciaGeracaoIa.idAtual()).isNull();
    }
}
