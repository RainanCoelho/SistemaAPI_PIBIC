package com.SistemaApiCrud.SistemaCrud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.SistemaApiCrud.SistemaCrud.exception.MuitasTentativasLoginException;

@SpringBootTest
@ActiveProfiles("test")
class LoginAttemptServiceTests {

    private static final Clock RELOGIO_FIXO = Clock.fixed(
            Instant.parse("2026-07-29T15:00:00Z"),
            ZoneOffset.UTC);

    @Autowired
    private LoginAttemptStore store;

    @Autowired
    private IdentificadorProtegido identificadorProtegido;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void limparTentativas() {
        jdbcTemplate.update("DELETE FROM tentativa_login");
    }

    @Test
    void deveCompartilharBloqueioEntreInstanciasConcorrentes() throws Exception {
        LoginAttemptService primeiraInstancia = service(2);
        LoginAttemptService segundaInstancia = service(2);
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var primeira = executor.submit(() -> {
                registrarFalhaConcorrente(
                        primeiraInstancia, "Professor", prontas, iniciar);
                return null;
            });
            var segunda = executor.submit(() -> {
                registrarFalhaConcorrente(
                        segundaInstancia, "professor", prontas, iniciar);
                return null;
            });
            assertThat(prontas.await(2, TimeUnit.SECONDS)).isTrue();
            iniciar.countDown();
            primeira.get(2, TimeUnit.SECONDS);
            segunda.get(2, TimeUnit.SECONDS);
        } finally {
            iniciar.countDown();
        }

        assertThatThrownBy(() -> primeiraInstancia.validarPermitido(
                        "PROFESSOR", "192.0.2.10"))
                .isInstanceOfSatisfying(MuitasTentativasLoginException.class, falha ->
                        assertThat(falha.getRetryAfterSeconds()).isEqualTo(900));
    }

    private void registrarFalhaConcorrente(
            LoginAttemptService service,
            String username,
            CountDownLatch prontas,
            CountDownLatch iniciar) throws InterruptedException {
        prontas.countDown();
        if (!iniciar.await(2, TimeUnit.SECONDS)) {
            throw new IllegalStateException("As tentativas concorrentes nao foram iniciadas");
        }
        service.registrarFalha(username, "192.0.2.10");
    }

    @Test
    void deveRemoverBloqueioDaContaAposSucessoSemPersistirDadoBruto() {
        LoginAttemptService service = service(3);
        service.registrarFalha("usuario.sensivel", "198.51.100.20");

        assertThat(jdbcTemplate.queryForList(
                        "SELECT identificador_hash FROM tentativa_login",
                        String.class))
                .hasSize(2)
                .allSatisfy(hash -> assertThat(hash)
                        .hasSize(64)
                        .doesNotContain("usuario", "198.51"));

        service.registrarSucesso("USUARIO.SENSIVEL");
        assertThatCode(() -> service.validarPermitido(
                "usuario.sensivel", "203.0.113.1")).doesNotThrowAnyException();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tentativa_login WHERE tipo = 'CONTA'",
                Long.class)).isZero();
    }

    @Test
    void deveUsarHmacComSeparacaoPorChaveECategoria() {
        IdentificadorProtegido primeiraChave = new IdentificadorProtegido(
                "segredo-um-com-material-suficiente-para-o-teste-de-hmac-2026");
        IdentificadorProtegido segundaChave = new IdentificadorProtegido(
                "segredo-dois-com-material-suficiente-para-o-teste-de-hmac-2026");

        String conta = primeiraChave.gerar("CONTA", "usuario");
        assertThat(conta).hasSize(64);
        assertThat(primeiraChave.gerar("CONTA", "usuario")).isEqualTo(conta);
        assertThat(primeiraChave.gerar("IP", "usuario")).isNotEqualTo(conta);
        assertThat(segundaChave.gerar("CONTA", "usuario")).isNotEqualTo(conta);
    }

    private LoginAttemptService service(int maximoTentativas) {
        return new LoginAttemptService(
                store,
                identificadorProtegido,
                maximoTentativas,
                15,
                RELOGIO_FIXO);
    }
}
