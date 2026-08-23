package com.SistemaApiCrud.SistemaCrud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.SistemaApiCrud.SistemaCrud.entity.SolicitacaoGeracaoIa;
import com.SistemaApiCrud.SistemaCrud.entity.Usuario;
import com.SistemaApiCrud.SistemaCrud.entity.enums.EstadoSolicitacaoGeracaoIa;
import com.SistemaApiCrud.SistemaCrud.entity.enums.OperacaoGeracaoIa;
import com.SistemaApiCrud.SistemaCrud.repository.SolicitacaoGeracaoIaRepository;

class IdempotenciaGeracaoIaStoreTests {

    private final SolicitacaoGeracaoIaRepository repository = mock(SolicitacaoGeracaoIaRepository.class);
    private final IdempotenciaGeracaoIaStore store = new IdempotenciaGeracaoIaStore(
            repository,
            Duration.ofHours(1));

    @Test
    void deveReutilizarRegistroExpiradoSemApagar() {
        Usuario usuario = new Usuario();
        usuario.setId(3L);
        SolicitacaoGeracaoIa anterior = new SolicitacaoGeracaoIa();
        anterior.setExpiraEm(Instant.now().minusSeconds(1));
        anterior.setEstado(EstadoSolicitacaoGeracaoIa.CONCLUIDA);
        anterior.setStatusResposta(200);
        anterior.setIdsResultado("10");
        when(repository.findByUsuarioIdAndChaveIdempotenciaForUpdate(3L, "chave"))
                .thenReturn(Optional.of(anterior));

        InicioIdempotenciaIa inicio = store.iniciar(
                usuario,
                "chave",
                "a".repeat(64),
                OperacaoGeracaoIa.GERAR_PERGUNTAS,
                7L);

        assertThat(inicio.criada()).isTrue();
        assertThat(anterior.getEstado()).isEqualTo(EstadoSolicitacaoGeracaoIa.EM_ANDAMENTO);
        assertThat(anterior.getIdsResultado()).isNull();
        assertThat(anterior.getStatusResposta()).isNull();
        assertThat(anterior.getExpiraEm()).isAfter(Instant.now().plusSeconds(3500));
    }

    @Test
    void deveReinicializarRegistroLegadoSemExpiracaoSobBloqueio() {
        Usuario usuario = new Usuario();
        usuario.setId(3L);
        SolicitacaoGeracaoIa anterior = new SolicitacaoGeracaoIa();
        anterior.setEstado(EstadoSolicitacaoGeracaoIa.CONCLUIDA);
        when(repository.findByUsuarioIdAndChaveIdempotenciaForUpdate(3L, "chave"))
                .thenReturn(Optional.of(anterior));

        InicioIdempotenciaIa inicio = store.iniciar(
                usuario,
                "chave",
                "a".repeat(64),
                OperacaoGeracaoIa.GERAR_PERGUNTAS,
                7L);

        assertThat(inicio.criada()).isTrue();
        assertThat(anterior.getEstado()).isEqualTo(EstadoSolicitacaoGeracaoIa.EM_ANDAMENTO);
        assertThat(anterior.getExpiraEm()).isAfter(Instant.now());
        verify(repository).findByUsuarioIdAndChaveIdempotenciaForUpdate(3L, "chave");
    }

    @Test
    void deveConcluirComApenasIdsDeResultado() {
        SolicitacaoGeracaoIa solicitacao = new SolicitacaoGeracaoIa();
        solicitacao.setEstado(EstadoSolicitacaoGeracaoIa.EM_ANDAMENTO);
        when(repository.findById(9L)).thenReturn(Optional.of(solicitacao));

        store.concluirNaTransacaoAtual(9L, 201, List.of(4L, 8L));

        assertThat(solicitacao.getEstado()).isEqualTo(EstadoSolicitacaoGeracaoIa.CONCLUIDA);
        assertThat(solicitacao.getIdsResultado()).isEqualTo("4,8");
        assertThat(solicitacao.getStatusResposta()).isEqualTo(201);
        verify(repository).findById(9L);
        verify(repository, org.mockito.Mockito.never()).save(any());
    }
}
