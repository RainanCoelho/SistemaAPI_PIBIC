package com.SistemaApiCrud.SistemaCrud.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.SistemaApiCrud.SistemaCrud.entity.SolicitacaoGeracaoIa;
import com.SistemaApiCrud.SistemaCrud.entity.Usuario;
import com.SistemaApiCrud.SistemaCrud.entity.enums.EstadoSolicitacaoGeracaoIa;
import com.SistemaApiCrud.SistemaCrud.entity.enums.OperacaoGeracaoIa;
import com.SistemaApiCrud.SistemaCrud.entity.enums.PapelUsuario;
import com.SistemaApiCrud.SistemaCrud.repository.SolicitacaoGeracaoIaRepository;
import com.SistemaApiCrud.SistemaCrud.repository.UsuarioRepository;

@SpringBootTest
class IdempotenciaGeracaoIaStoreConcurrencyTests {

    @Autowired
    private IdempotenciaGeracaoIaStore store;

    @Autowired
    private SolicitacaoGeracaoIaRepository solicitacaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void devePermitirApenasUmaReinicializacaoConcorrenteDoRegistroExpirado() throws Exception {
        String sufixo = UUID.randomUUID().toString();
        Usuario usuario = usuarioRepository.saveAndFlush(new Usuario(
                null,
                "idempotencia-concorrente-" + sufixo,
                "senha",
                PapelUsuario.PROFESSOR,
                true,
                0L,
                null,
                null));
        SolicitacaoGeracaoIa solicitacao = new SolicitacaoGeracaoIa();
        solicitacao.setUsuario(usuario);
        solicitacao.setChaveIdempotencia(UUID.randomUUID().toString());
        solicitacao.setHashRequisicao("a".repeat(64));
        solicitacao.setOperacao(OperacaoGeracaoIa.GERAR_PERGUNTAS);
        solicitacao.setEstado(EstadoSolicitacaoGeracaoIa.CONCLUIDA);
        solicitacao.setStatusResposta(201);
        solicitacao.setIdsResultado("1");
        solicitacao.setCriadoEm(Instant.now().minusSeconds(7200));
        solicitacao.setAtualizadoEm(Instant.now().minusSeconds(3600));
        solicitacao.setExpiraEm(Instant.now().minusSeconds(1));
        solicitacaoRepository.saveAndFlush(solicitacao);

        CountDownLatch partida = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> resultados = List.of(
                    executor.submit(() -> iniciarAposPartida(partida, usuario, solicitacao.getChaveIdempotencia())),
                    executor.submit(() -> iniciarAposPartida(partida, usuario, solicitacao.getChaveIdempotencia())));
            partida.countDown();

            assertThat(resultados.stream().filter(resultado -> obter(resultado)).count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean iniciarAposPartida(CountDownLatch partida, Usuario usuario, String chave) throws InterruptedException {
        partida.await();
        return store.iniciar(
                usuario,
                chave,
                "b".repeat(64),
                OperacaoGeracaoIa.GERAR_PERGUNTAS,
                null).criada();
    }

    private boolean obter(Future<Boolean> resultado) {
        try {
            return resultado.get();
        } catch (Exception ex) {
            throw new AssertionError("A reinicializacao concorrente falhou", ex);
        }
    }
}
