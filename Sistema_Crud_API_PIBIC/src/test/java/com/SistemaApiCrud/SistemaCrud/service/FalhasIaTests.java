package com.SistemaApiCrud.SistemaCrud.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

class FalhasIaTests {

    @Test
    void deveReconhecerConexaoRecusadaMesmoQuandoEncapsulada() {
        RuntimeException falha = new CompletionException(
                new IllegalStateException(new ConnectException("Connection refused")));

        assertThat(FalhasIa.possuiIndisponibilidadeDeRede(falha)).isTrue();
    }

    @Test
    void deveReconhecerHostDesconhecido() {
        assertThat(FalhasIa.possuiIndisponibilidadeDeRede(
                new UnknownHostException("gateway-inexistente"))).isTrue();
    }

    @Test
    void naoDeveClassificarErroDeFormatoComoIndisponibilidadeDeRede() {
        assertThat(FalhasIa.possuiIndisponibilidadeDeRede(
                new IllegalArgumentException("resposta invalida"))).isFalse();
    }
}
