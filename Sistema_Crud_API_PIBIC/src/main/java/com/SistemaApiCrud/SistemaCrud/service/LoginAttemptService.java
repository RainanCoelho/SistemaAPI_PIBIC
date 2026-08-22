package com.SistemaApiCrud.SistemaCrud.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.SistemaApiCrud.SistemaCrud.exception.MuitasTentativasLoginException;
import com.SistemaApiCrud.SistemaCrud.service.LoginAttemptStore.EstadoTentativaLogin;

import jakarta.annotation.PostConstruct;

@Service
public class LoginAttemptService {

    private static final String TIPO_CONTA = "CONTA";
    private static final String TIPO_IP = "IP";

    private final LoginAttemptStore store;
    private final IdentificadorProtegido identificadorProtegido;
    private final AtomicLong falhasRegistradas = new AtomicLong();
    private final int maxAttempts;
    private final Duration lockDuration;
    private final Clock relogio;

    @Autowired
    public LoginAttemptService(
            LoginAttemptStore store,
            IdentificadorProtegido identificadorProtegido,
            @Value("${app.security.login.max-attempts:5}") int maxAttempts,
            @Value("${app.security.login.lock-minutes:15}") long lockMinutes) {
        this(store, identificadorProtegido, maxAttempts, lockMinutes, Clock.systemUTC());
    }

    LoginAttemptService(
            LoginAttemptStore store,
            IdentificadorProtegido identificadorProtegido,
            int maxAttempts,
            long lockMinutes,
            Clock relogio) {
        if (maxAttempts < 1 || lockMinutes < 1) {
            throw new IllegalArgumentException("Os limites de tentativa de login devem ser positivos");
        }
        this.store = store;
        this.identificadorProtegido = identificadorProtegido;
        this.maxAttempts = maxAttempts;
        this.lockDuration = Duration.ofMinutes(lockMinutes);
        this.relogio = relogio;
    }

    @PostConstruct
    void removerTentativasAntigasAoIniciar() {
        Instant agora = relogio.instant();
        store.removerExpirados(agora.minus(lockDuration), agora);
    }

    public void validarPermitido(String username, String remoteAddress) {
        Instant agora = relogio.instant();
        validarBloqueio(TIPO_CONTA, hashConta(username), agora);
        validarBloqueio(TIPO_IP, hashIp(remoteAddress), agora);
    }

    public void registrarFalha(String username, String remoteAddress) {
        Instant agora = relogio.instant();
        Instant inicioJanela = agora.minus(lockDuration);
        Instant bloqueadoAte = agora.plus(lockDuration);
        store.registrarFalha(
                TIPO_CONTA,
                hashConta(username),
                agora,
                inicioJanela,
                bloqueadoAte,
                maxAttempts);
        store.registrarFalha(
                TIPO_IP,
                hashIp(remoteAddress),
                agora,
                inicioJanela,
                bloqueadoAte,
                maxAttempts * 5);

        if (falhasRegistradas.incrementAndGet() % 100 == 0) {
            store.removerExpirados(inicioJanela, agora);
        }
    }

    public void registrarSucesso(String username) {
        store.remover(TIPO_CONTA, hashConta(username));
    }

    private void validarBloqueio(String tipo, String identificadorHash, Instant agora) {
        EstadoTentativaLogin tentativas = store.buscar(tipo, identificadorHash).orElse(null);
        if (tentativas == null) {
            return;
        }

        Instant inicioJanela = agora.minus(lockDuration);
        if (tentativas.ultimaTentativa().isBefore(inicioJanela)) {
            store.removerSeExpirado(tipo, identificadorHash, inicioJanela, agora);
            return;
        }

        if (tentativas.bloqueadoAte() != null && tentativas.bloqueadoAte().isAfter(agora)) {
            throw new MuitasTentativasLoginException(
                    Math.max(1, Duration.between(agora, tentativas.bloqueadoAte()).toSeconds()));
        }
    }

    private String hashConta(String username) {
        String normalizado = username == null
                ? "<vazio>"
                : username.trim().toLowerCase(Locale.ROOT);
        return identificadorProtegido.gerar(TIPO_CONTA, normalizado);
    }

    private String hashIp(String remoteAddress) {
        String normalizado = remoteAddress == null || remoteAddress.isBlank()
                ? "<desconhecido>"
                : remoteAddress.trim();
        return identificadorProtegido.gerar(TIPO_IP, normalizado);
    }
}
