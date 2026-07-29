package com.SistemaApiCrud.SistemaCrud.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.SistemaApiCrud.SistemaCrud.exception.LimiteUsoIaException;

@Component
public class ControleUsoIa {

    private static final ZoneId FUSO_HORARIO_PADRAO = ZoneId.of("America/Sao_Paulo");

    private final int limitePorMinuto;
    private final int limitePorDia;
    private final Semaphore vagasSimultaneas;
    private final Clock relogio;
    private final Map<String, EstadoUso> usosPorUsuario = new ConcurrentHashMap<>();

    @Autowired
    public ControleUsoIa(
            @Value("${app.ia.limite-por-minuto:5}") int limitePorMinuto,
            @Value("${app.ia.limite-por-dia:20}") int limitePorDia,
            @Value("${app.ia.maximo-simultaneas:3}") int maximoSimultaneas) {
        this(limitePorMinuto, limitePorDia, maximoSimultaneas, Clock.system(FUSO_HORARIO_PADRAO));
    }

    ControleUsoIa(int limitePorMinuto, int limitePorDia, int maximoSimultaneas, Clock relogio) {
        if (limitePorMinuto < 1 || limitePorDia < 1 || maximoSimultaneas < 1) {
            throw new IllegalArgumentException("Os limites de uso da IA devem ser maiores que zero");
        }
        this.limitePorMinuto = limitePorMinuto;
        this.limitePorDia = limitePorDia;
        this.vagasSimultaneas = new Semaphore(maximoSimultaneas, true);
        this.relogio = relogio;
    }

    public <T> T executar(OperacaoIa<T> operacao) {
        if (!vagasSimultaneas.tryAcquire()) {
            throw new LimiteUsoIaException(
                    "O limite de geracoes simultaneas foi atingido; tente novamente em instantes",
                    1);
        }

        try {
            registrarUso(identificarUsuario());
            return operacao.executar();
        } finally {
            vagasSimultaneas.release();
        }
    }

    private void registrarUso(String identificadorUsuario) {
        Instant agora = relogio.instant();
        long minutoAtual = agora.getEpochSecond() / 60;
        LocalDate diaAtual = LocalDate.ofInstant(agora, relogio.getZone());
        EstadoUso estado = usosPorUsuario.computeIfAbsent(
                identificadorUsuario,
                identificador -> new EstadoUso(minutoAtual, diaAtual));

        synchronized (estado) {
            if (estado.minuto != minutoAtual) {
                estado.minuto = minutoAtual;
                estado.usosNoMinuto = 0;
            }
            if (!estado.dia.equals(diaAtual)) {
                estado.dia = diaAtual;
                estado.usosNoDia = 0;
            }

            if (estado.usosNoMinuto >= limitePorMinuto) {
                long segundosRestantes = 60 - Math.floorMod(agora.getEpochSecond(), 60);
                throw new LimiteUsoIaException(
                        "O limite de geracoes de IA por minuto foi atingido",
                        segundosRestantes);
            }
            if (estado.usosNoDia >= limitePorDia) {
                Instant inicioProximoDia = diaAtual.plusDays(1)
                        .atStartOfDay(relogio.getZone())
                        .toInstant();
                long segundosRestantes = Duration.between(agora, inicioProximoDia).toSeconds();
                throw new LimiteUsoIaException(
                        "O limite diario de geracoes de IA foi atingido",
                        segundosRestantes);
            }

            estado.usosNoMinuto++;
            estado.usosNoDia++;
        }
    }

    private String identificarUsuario() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null
                || !autenticacao.isAuthenticated()
                || autenticacao.getName() == null
                || autenticacao.getName().isBlank()) {
            return "processo-interno";
        }
        return autenticacao.getName();
    }

    @FunctionalInterface
    public interface OperacaoIa<T> {

        T executar();
    }

    private static final class EstadoUso {

        private long minuto;
        private int usosNoMinuto;
        private LocalDate dia;
        private int usosNoDia;

        private EstadoUso(long minuto, LocalDate dia) {
            this.minuto = minuto;
            this.dia = dia;
        }
    }
}
