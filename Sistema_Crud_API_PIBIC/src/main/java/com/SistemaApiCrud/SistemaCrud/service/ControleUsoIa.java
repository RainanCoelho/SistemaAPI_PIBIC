package com.SistemaApiCrud.SistemaCrud.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.SistemaApiCrud.SistemaCrud.exception.LimiteUsoIaException;
import com.SistemaApiCrud.SistemaCrud.service.ControleUsoIaStore.ResultadoCota;

import jakarta.annotation.PostConstruct;

@Component
public class ControleUsoIa {

    private static final ZoneId FUSO_HORARIO_PADRAO = ZoneId.of("America/Sao_Paulo");
    private static final Logger LOGGER = LoggerFactory.getLogger(ControleUsoIa.class);
    private static final Duration MARGEM_TIMEOUT_MAXIMA = Duration.ofSeconds(10);
    private static final Duration MARGEM_TIMEOUT_MINIMA = Duration.ofMillis(100);

    private final ControleUsoIaStore store;
    private final IdentificadorProtegido identificadorProtegido;
    private final boolean limitesHabilitados;
    private final int limitePorMinuto;
    private final int limitePorDia;
    private final int maximoSimultaneas;
    private final Duration duracaoLease;
    private final Clock relogio;
    private final AtomicLong usosRegistrados = new AtomicLong();

    @Autowired
    public ControleUsoIa(
            ControleUsoIaStore store,
            IdentificadorProtegido identificadorProtegido,
            @Value("${app.ia.limites-habilitados:false}") boolean limitesHabilitados,
            @Value("${app.ia.limite-por-minuto:5}") int limitePorMinuto,
            @Value("${app.ia.limite-por-dia:20}") int limitePorDia,
            @Value("${app.ia.maximo-simultaneas:3}") int maximoSimultaneas,
            @Value("${app.ia.lease-minutes:10}") long leaseMinutes,
            @Value("${spring.ai.openai.timeout:60s}") Duration tempoLimiteOperacao) {
        this(
                store,
                identificadorProtegido,
                limitePorMinuto,
                limitePorDia,
                maximoSimultaneas,
                Duration.ofMinutes(leaseMinutes),
                tempoLimiteOperacao,
                Clock.system(FUSO_HORARIO_PADRAO),
                limitesHabilitados);
    }

    ControleUsoIa(
            ControleUsoIaStore store,
            IdentificadorProtegido identificadorProtegido,
            int limitePorMinuto,
            int limitePorDia,
            int maximoSimultaneas,
            Duration duracaoLease,
            Duration tempoLimiteOperacao,
            Clock relogio) {
        this(
                store,
                identificadorProtegido,
                limitePorMinuto,
                limitePorDia,
                maximoSimultaneas,
                duracaoLease,
                tempoLimiteOperacao,
                relogio,
                true);
    }

    ControleUsoIa(
            ControleUsoIaStore store,
            IdentificadorProtegido identificadorProtegido,
            int limitePorMinuto,
            int limitePorDia,
            int maximoSimultaneas,
            Duration duracaoLease,
            Duration tempoLimiteOperacao,
            Clock relogio,
            boolean limitesHabilitados) {
        if (limitePorMinuto < 1
                || limitePorDia < 1
                || maximoSimultaneas < 1
                || duracaoLease == null
                || duracaoLease.isNegative()
                || duracaoLease.isZero()
                || tempoLimiteOperacao == null
                || tempoLimiteOperacao.isNegative()
                || tempoLimiteOperacao.isZero()) {
            throw new IllegalArgumentException("Os limites de uso da IA devem ser maiores que zero");
        }
        if (duracaoLease.compareTo(
                tempoLimiteOperacao.plus(margemTimeout(tempoLimiteOperacao))) <= 0) {
            throw new IllegalArgumentException(
                    "A duracao da vaga de IA deve superar o timeout do provedor com margem de seguranca");
        }
        this.store = store;
        this.identificadorProtegido = identificadorProtegido;
        this.limitesHabilitados = limitesHabilitados;
        this.limitePorMinuto = limitePorMinuto;
        this.limitePorDia = limitePorDia;
        this.maximoSimultaneas = maximoSimultaneas;
        this.duracaoLease = duracaoLease;
        this.relogio = relogio;
    }

    private Duration margemTimeout(Duration tempoLimiteOperacao) {
        Duration margem = tempoLimiteOperacao.dividedBy(4);
        if (margem.compareTo(MARGEM_TIMEOUT_MINIMA) < 0) {
            return MARGEM_TIMEOUT_MINIMA;
        }
        return margem.compareTo(MARGEM_TIMEOUT_MAXIMA) > 0
                ? MARGEM_TIMEOUT_MAXIMA
                : margem;
    }

    @PostConstruct
    void removerCotasAntigasAoIniciar() {
        if (limitesHabilitados) {
            store.removerCotasAnterioresA(relogio.instant().minus(2, ChronoUnit.DAYS));
        }
    }

    public <T> T executar(OperacaoIa<T> operacao) {
        if (!limitesHabilitados) {
            return operacao.executar();
        }
        Instant agora = relogio.instant();
        String identificadorHash = identificadorProtegido.gerar(
                "USO_IA",
                identificarUsuario());
        String lease = store.adquirirLease(identificadorHash, duracaoLease, maximoSimultaneas)
                .orElseThrow(() -> new LimiteUsoIaException(
                        "O limite de geracoes simultaneas foi atingido; tente novamente em instantes",
                        1));
        AtomicBoolean concluida = new AtomicBoolean();
        Thread renovador = iniciarRenovacaoLease(lease, concluida);

        try {
            if (ContextoIdempotenciaGeracaoIa.deveRegistrarUso()) {
                registrarUso(identificadorHash, agora);
                ContextoIdempotenciaGeracaoIa.marcarUsoRegistrado();
            }
            return operacao.executar();
        } finally {
            concluida.set(true);
            renovador.interrupt();
            store.liberarLease(lease);
        }
    }

    private Thread iniciarRenovacaoLease(String lease, AtomicBoolean concluida) {
        Duration intervalo = duracaoLease.dividedBy(3);
        if (intervalo.isZero()) {
            intervalo = Duration.ofMillis(1);
        }
        Duration intervaloRenovacao = intervalo;
        return Thread.ofVirtual()
                .name("renovacao-lease-ia")
                .start(() -> {
                    while (!concluida.get()) {
                        try {
                            Thread.sleep(intervaloRenovacao);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                        if (!concluida.get()) {
                            try {
                                if (!store.renovarLease(lease, duracaoLease)) {
                                    LOGGER.warn("A vaga de IA {} expirou antes da renovacao", lease);
                                    return;
                                }
                            } catch (RuntimeException ex) {
                                LOGGER.error("Falha ao renovar a vaga de IA {}", lease, ex);
                                return;
                            }
                        }
                    }
                });
    }

    private void registrarUso(String identificadorHash, Instant agora) {
        Instant inicioMinuto = agora.truncatedTo(ChronoUnit.MINUTES);
        LocalDate diaAtual = LocalDate.ofInstant(agora, relogio.getZone());
        ResultadoCota resultado = store.registrarUso(
                identificadorHash,
                inicioMinuto,
                diaAtual,
                limitePorMinuto,
                limitePorDia);

        if (resultado == ResultadoCota.LIMITE_MINUTO) {
            long segundosRestantes = 60 - Math.floorMod(agora.getEpochSecond(), 60);
            throw new LimiteUsoIaException(
                    "O limite de geracoes de IA por minuto foi atingido",
                    segundosRestantes);
        }
        if (resultado == ResultadoCota.LIMITE_DIA) {
            Instant inicioProximoDia = diaAtual.plusDays(1)
                    .atStartOfDay(relogio.getZone())
                    .toInstant();
            long segundosRestantes = Duration.between(agora, inicioProximoDia).toSeconds();
            throw new LimiteUsoIaException(
                    "O limite diario de geracoes de IA foi atingido",
                    segundosRestantes);
        }
        if (usosRegistrados.incrementAndGet() % 100 == 0) {
            store.removerCotasAnterioresA(agora.minus(2, ChronoUnit.DAYS));
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
}
