package com.SistemaApiCrud.SistemaCrud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import com.SistemaApiCrud.SistemaCrud.exception.LimiteUsoIaException;
import com.SistemaApiCrud.SistemaCrud.service.ControleUsoIaStore.ResultadoCota;

@SpringBootTest
@ActiveProfiles("test")
class ControleUsoIaTests {

    private static final ZoneId FUSO_HORARIO = ZoneId.of("America/Sao_Paulo");
    private static final Clock RELOGIO_FIXO = Clock.fixed(
            Instant.parse("2026-07-29T15:00:00Z"),
            FUSO_HORARIO);

    @Autowired
    private ControleUsoIaStore store;

    @Autowired
    private IdentificadorProtegido identificadorProtegido;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void limparEstadoPersistido() {
        jdbcTemplate.update("DELETE FROM lease_uso_ia");
        jdbcTemplate.update("DELETE FROM cota_uso_ia");
    }

    @AfterEach
    void limparAutenticacao() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveLimitarGeracoesPorMinutoEntreInstancias() {
        autenticar("professor-a");
        ControleUsoIa primeiraInstancia = controle(2, 20, 3);
        ControleUsoIa segundaInstancia = controle(2, 20, 3);

        assertThat(primeiraInstancia.executar(() -> "primeira")).isEqualTo("primeira");
        assertThat(segundaInstancia.executar(() -> "segunda")).isEqualTo("segunda");
        assertThatThrownBy(() -> primeiraInstancia.executar(() -> "terceira"))
                .isInstanceOfSatisfying(LimiteUsoIaException.class, falha -> {
                    assertThat(falha.getMessage()).contains("por minuto");
                    assertThat(falha.getSegundosAteNovaTentativa()).isEqualTo(60);
                });
    }

    @Test
    void deveLimitarGeracoesPorDia() {
        autenticar("professor-a");
        ControleUsoIa controle = controle(10, 2, 3);

        controle.executar(() -> null);
        controle.executar(() -> null);

        assertThatThrownBy(() -> controle.executar(() -> null))
                .isInstanceOfSatisfying(LimiteUsoIaException.class, falha -> {
                    assertThat(falha.getMessage()).contains("diario");
                    assertThat(falha.getSegundosAteNovaTentativa()).isPositive();
                });
    }

    @Test
    void deveIgnorarTodasAsCotasQuandoOsLimitesEstaoDesabilitados() {
        ControleUsoIa controle = new ControleUsoIa(
                store,
                identificadorProtegido,
                1,
                1,
                1,
                Duration.ofMinutes(10),
                Duration.ofSeconds(40),
                RELOGIO_FIXO,
                false);

        assertThat(controle.executar(() -> "primeira")).isEqualTo("primeira");
        assertThat(controle.executar(() -> "segunda")).isEqualTo("segunda");
        assertThat(controle.executar(() -> "terceira")).isEqualTo("terceira");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cota_uso_ia",
                Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lease_uso_ia",
                Integer.class)).isZero();
    }

    @Test
    void deveContabilizarUmaOperacaoMesmoComMultiplasChamadasInternas() {
        autenticar("professor-a");
        ControleUsoIa controle = controle(1, 10, 3);

        String resultado = ContextoIdempotenciaGeracaoIa.executar(91L, () -> {
            controle.executar(() -> "primeira chamada interna");
            return controle.executar(() -> "segunda chamada interna");
        });

        assertThat(resultado).isEqualTo("segunda chamada interna");
        assertThat(ContextoIdempotenciaGeracaoIa.idAtual()).isNull();
        assertThatThrownBy(() -> ContextoIdempotenciaGeracaoIa.executar(
                92L,
                () -> controle.executar(() -> "nova geracao")))
                .isInstanceOf(LimiteUsoIaException.class);
    }

    @Test
    void deveManterCotaSeparadaEIdentificadorProtegidoPorUsuario() {
        ControleUsoIa controle = controle(1, 1, 3);

        autenticar("professor-a");
        assertThat(controle.executar(() -> "a")).isEqualTo("a");
        autenticar("professor-b");
        assertThat(controle.executar(() -> "b")).isEqualTo("b");

        assertThat(jdbcTemplate.queryForList(
                        "SELECT identificador_hash FROM cota_uso_ia",
                        String.class))
                .hasSize(2)
                .allSatisfy(hash -> assertThat(hash)
                        .hasSize(64)
                        .doesNotContain("professor"));
    }

    @Test
    void deveCompartilharLimiteSimultaneoELiberarLeaseAposFalha() {
        String primeiro = store.adquirirLease("hash-a", Duration.ofMinutes(10), 1)
                .orElseThrow();
        assertThat(store.adquirirLease("hash-b", Duration.ofMinutes(10), 1)).isEmpty();
        store.liberarLease(primeiro);
        assertThat(store.adquirirLease("hash-b", Duration.ofMinutes(10), 1)).isPresent();
    }

    @Test
    void deveRenovarLeaseEnquantoOperacaoPermaneceEmExecucao() throws Exception {
        autenticar("professor-lento");
        ControleUsoIa controle = new ControleUsoIa(
                store,
                identificadorProtegido,
                10,
                10,
                1,
                Duration.ofMillis(900),
                Duration.ofMillis(100),
                RELOGIO_FIXO);
        CountDownLatch iniciou = new CountDownLatch(1);
        CountDownLatch concluir = new CountDownLatch(1);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var resultado = executor.submit(() -> controle.executar(() -> {
                iniciou.countDown();
                try {
                    if (!concluir.await(3, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("A operacao de teste nao foi liberada");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ex);
                }
                return "concluida";
            }));

            assertThat(iniciou.await(2, TimeUnit.SECONDS)).isTrue();
            Thread.sleep(Duration.ofMillis(1_200));
            assertThat(store.adquirirLease(
                    "hash-concorrente",
                    Duration.ofSeconds(1),
                    1)).isEmpty();
            concluir.countDown();
            assertThat(resultado.get(2, TimeUnit.SECONDS)).isEqualTo("concluida");
        } finally {
            concluir.countDown();
        }
    }

    @Test
    void deveSerializarConsumoConcorrenteDaMesmaCota() throws Exception {
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        Instant minuto = Instant.parse("2026-07-29T15:00:00Z");

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var primeira = executor.submit(() -> registrarUsoConcorrente(
                    prontas, iniciar, minuto));
            var segunda = executor.submit(() -> registrarUsoConcorrente(
                    prontas, iniciar, minuto));
            assertThat(prontas.await(2, TimeUnit.SECONDS)).isTrue();
            iniciar.countDown();

            assertThat(java.util.List.of(
                            primeira.get(2, TimeUnit.SECONDS),
                            segunda.get(2, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(
                            ResultadoCota.PERMITIDO,
                            ResultadoCota.LIMITE_MINUTO);
        } finally {
            iniciar.countDown();
        }
    }

    @Test
    void deveExigirLeaseMaiorQueTimeoutDoProvedorComMargem() {
        assertThatThrownBy(() -> new ControleUsoIa(
                store,
                identificadorProtegido,
                5,
                20,
                3,
                Duration.ofSeconds(45),
                Duration.ofSeconds(40),
                RELOGIO_FIXO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timeout");
    }

    private ResultadoCota registrarUsoConcorrente(
            CountDownLatch prontas,
            CountDownLatch iniciar,
            Instant minuto) throws InterruptedException {
        prontas.countDown();
        if (!iniciar.await(2, TimeUnit.SECONDS)) {
            throw new IllegalStateException("As operacoes concorrentes nao foram iniciadas");
        }
        return store.registrarUso(
                "mesmo-hash",
                minuto,
                LocalDate.of(2026, 7, 29),
                1,
                10);
    }

    private ControleUsoIa controle(int limiteMinuto, int limiteDia, int simultaneas) {
        return new ControleUsoIa(
                store,
                identificadorProtegido,
                limiteMinuto,
                limiteDia,
                simultaneas,
                Duration.ofMinutes(10),
                Duration.ofSeconds(40),
                RELOGIO_FIXO);
    }

    private void autenticar(String usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        usuario,
                        "credencial",
                        java.util.List.of()));
    }
}
