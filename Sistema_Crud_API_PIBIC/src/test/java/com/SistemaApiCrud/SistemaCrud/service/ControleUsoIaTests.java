package com.SistemaApiCrud.SistemaCrud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.SistemaApiCrud.SistemaCrud.exception.LimiteUsoIaException;

class ControleUsoIaTests {

    private static final ZoneId FUSO_HORARIO = ZoneId.of("America/Sao_Paulo");
    private static final Clock RELOGIO_FIXO = Clock.fixed(
            Instant.parse("2026-07-29T15:00:00Z"),
            FUSO_HORARIO);

    @AfterEach
    void limparAutenticacao() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveLimitarGeracoesPorMinuto() {
        autenticar("professor-a");
        ControleUsoIa controle = new ControleUsoIa(2, 20, 3, RELOGIO_FIXO);

        assertThat(controle.executar(() -> "primeira")).isEqualTo("primeira");
        assertThat(controle.executar(() -> "segunda")).isEqualTo("segunda");
        assertThatThrownBy(() -> controle.executar(() -> "terceira"))
                .isInstanceOfSatisfying(LimiteUsoIaException.class, falha -> {
                    assertThat(falha.getMessage()).contains("por minuto");
                    assertThat(falha.getSegundosAteNovaTentativa()).isEqualTo(60);
                });
    }

    @Test
    void deveLimitarGeracoesPorDia() {
        autenticar("professor-a");
        ControleUsoIa controle = new ControleUsoIa(10, 2, 3, RELOGIO_FIXO);

        controle.executar(() -> null);
        controle.executar(() -> null);

        assertThatThrownBy(() -> controle.executar(() -> null))
                .isInstanceOfSatisfying(LimiteUsoIaException.class, falha -> {
                    assertThat(falha.getMessage()).contains("diario");
                    assertThat(falha.getSegundosAteNovaTentativa()).isPositive();
                });
    }

    @Test
    void deveManterCotaSeparadaPorUsuario() {
        ControleUsoIa controle = new ControleUsoIa(1, 1, 3, RELOGIO_FIXO);

        autenticar("professor-a");
        assertThat(controle.executar(() -> "a")).isEqualTo("a");

        autenticar("professor-b");
        assertThat(controle.executar(() -> "b")).isEqualTo("b");
    }

    private void autenticar(String usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        usuario,
                        "credencial",
                        java.util.List.of()));
    }
}
