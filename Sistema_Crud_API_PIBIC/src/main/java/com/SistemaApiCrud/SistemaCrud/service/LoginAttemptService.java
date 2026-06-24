package com.SistemaApiCrud.SistemaCrud.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.SistemaApiCrud.SistemaCrud.exception.MuitasTentativasLoginException;

@Service
public class LoginAttemptService {

    private final ConcurrentMap<String, Tentativas> tentativasPorConta = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Tentativas> tentativasPorIp = new ConcurrentHashMap<>();
    private final AtomicLong falhasRegistradas = new AtomicLong();
    private final int maxAttempts;
    private final Duration lockDuration;

    public LoginAttemptService(
            @Value("${app.security.login.max-attempts:5}") int maxAttempts,
            @Value("${app.security.login.lock-minutes:15}") long lockMinutes) {
        this.maxAttempts = maxAttempts;
        this.lockDuration = Duration.ofMinutes(lockMinutes);
    }

    public void validarPermitido(String username, String remoteAddress) {
        Instant agora = Instant.now();
        validarBloqueio(tentativasPorConta, chaveConta(username), agora);
        validarBloqueio(tentativasPorIp, chaveIp(remoteAddress), agora);
    }

    public void registrarFalha(String username, String remoteAddress) {
        Instant agora = Instant.now();
        registrarFalha(tentativasPorConta, chaveConta(username), agora, maxAttempts);
        registrarFalha(tentativasPorIp, chaveIp(remoteAddress), agora, maxAttempts * 5);
        if (falhasRegistradas.incrementAndGet() % 100 == 0) {
            removerEntradasExpiradas(agora);
        }
    }

    public void registrarSucesso(String username) {
        tentativasPorConta.remove(chaveConta(username));
    }

    private void validarBloqueio(ConcurrentMap<String, Tentativas> mapa, String chave, Instant agora) {
        Tentativas tentativas = mapa.get(chave);
        if (tentativas == null) {
            return;
        }

        if (tentativas.ultimaTentativa().plus(lockDuration).isBefore(agora)) {
            mapa.remove(chave, tentativas);
            return;
        }

        if (tentativas.bloqueadoAte() != null && tentativas.bloqueadoAte().isAfter(agora)) {
            throw new MuitasTentativasLoginException(
                    Math.max(1, Duration.between(agora, tentativas.bloqueadoAte()).toSeconds()));
        }
    }

    private void registrarFalha(
            ConcurrentMap<String, Tentativas> mapa,
            String chave,
            Instant agora,
            int limite) {
        mapa.compute(chave, (ignorada, atual) -> {
            if (atual != null && atual.bloqueadoAte() != null && atual.bloqueadoAte().isAfter(agora)) {
                return atual;
            }

            boolean janelaExpirada = atual != null
                    && atual.ultimaTentativa().plus(lockDuration).isBefore(agora);
            int quantidade = atual == null || janelaExpirada ? 1 : atual.quantidade() + 1;
            Instant bloqueadoAte = quantidade >= limite ? agora.plus(lockDuration) : null;
            return new Tentativas(quantidade, bloqueadoAte, agora);
        });
    }

    private void removerEntradasExpiradas(Instant agora) {
        tentativasPorConta.entrySet().removeIf(entry ->
                entry.getValue().ultimaTentativa().plus(lockDuration).isBefore(agora));
        tentativasPorIp.entrySet().removeIf(entry ->
                entry.getValue().ultimaTentativa().plus(lockDuration).isBefore(agora));
    }

    private String chaveConta(String username) {
        if (username == null) {
            return "<vazio>";
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String chaveIp(String remoteAddress) {
        return remoteAddress == null || remoteAddress.isBlank() ? "<desconhecido>" : remoteAddress;
    }

    private record Tentativas(int quantidade, Instant bloqueadoAte, Instant ultimaTentativa) {
    }
}
