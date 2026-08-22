package com.SistemaApiCrud.SistemaCrud.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class LoginAttemptStore {

    private static final int TOTAL_SLOTS = 32;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public LoginAttemptStore(
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public Optional<EstadoTentativaLogin> buscar(String tipo, String identificadorHash) {
        return buscarInterno(tipo, identificadorHash);
    }

    public void registrarFalha(
            String tipo,
            String identificadorHash,
            Instant agora,
            Instant inicioJanela,
            Instant bloqueadoAte,
            int limite) {
        transactionTemplate.executeWithoutResult(status -> {
            bloquearSlot(identificadorHash);
            Optional<EstadoTentativaLogin> atual = buscarInterno(tipo, identificadorHash);
            if (atual.isEmpty()) {
                inserir(tipo, identificadorHash, agora, limite == 1 ? bloqueadoAte : null);
                return;
            }

            EstadoTentativaLogin estado = atual.get();
            if (estado.bloqueadoAte() != null && estado.bloqueadoAte().isAfter(agora)) {
                return;
            }

            int quantidade = estado.ultimaTentativa().isBefore(inicioJanela)
                    ? 1
                    : estado.quantidade() + 1;
            Instant novoBloqueio = quantidade >= limite ? bloqueadoAte : null;
            jdbcTemplate.update(
                    """
                    UPDATE tentativa_login
                       SET quantidade = ?, ultima_tentativa = ?, bloqueado_ate = ?
                     WHERE tipo = ? AND identificador_hash = ?
                    """,
                    quantidade,
                    utc(agora),
                    novoBloqueio == null ? null : utc(novoBloqueio),
                    tipo,
                    identificadorHash);
        });
    }

    public void remover(String tipo, String identificadorHash) {
        transactionTemplate.executeWithoutResult(status -> {
            bloquearSlot(identificadorHash);
            jdbcTemplate.update(
                    "DELETE FROM tentativa_login WHERE tipo = ? AND identificador_hash = ?",
                    tipo,
                    identificadorHash);
        });
    }

    public void removerSeExpirado(
            String tipo,
            String identificadorHash,
            Instant inicioJanela,
            Instant agora) {
        jdbcTemplate.update(
                """
                DELETE FROM tentativa_login
                 WHERE tipo = ?
                   AND identificador_hash = ?
                   AND ultima_tentativa < ?
                   AND (bloqueado_ate IS NULL OR bloqueado_ate <= ?)
                """,
                tipo,
                identificadorHash,
                utc(inicioJanela),
                utc(agora));
    }

    public void removerExpirados(Instant inicioJanela, Instant agora) {
        jdbcTemplate.update(
                """
                DELETE FROM tentativa_login
                 WHERE ultima_tentativa < ?
                   AND (bloqueado_ate IS NULL OR bloqueado_ate <= ?)
                """,
                utc(inicioJanela),
                utc(agora));
    }

    private Optional<EstadoTentativaLogin> buscarInterno(String tipo, String identificadorHash) {
        List<EstadoTentativaLogin> resultados = jdbcTemplate.query(
                """
                SELECT quantidade, ultima_tentativa, bloqueado_ate
                  FROM tentativa_login
                 WHERE tipo = ? AND identificador_hash = ?
                """,
                (resultSet, rowNum) -> mapear(resultSet),
                tipo,
                identificadorHash);
        return resultados.stream().findFirst();
    }

    private void inserir(
            String tipo,
            String identificadorHash,
            Instant agora,
            Instant bloqueadoAte) {
        jdbcTemplate.update(
                """
                INSERT INTO tentativa_login
                    (tipo, identificador_hash, quantidade, ultima_tentativa, bloqueado_ate)
                VALUES (?, ?, 1, ?, ?)
                """,
                tipo,
                identificadorHash,
                utc(agora),
                bloqueadoAte == null ? null : utc(bloqueadoAte));
    }

    private void bloquearSlot(String identificadorHash) {
        int slot = Math.floorMod(identificadorHash.hashCode(), TOTAL_SLOTS);
        jdbcTemplate.queryForObject(
                "SELECT slot FROM coordenacao_tentativa_login WHERE slot = ? FOR UPDATE",
                Integer.class,
                slot);
    }

    private EstadoTentativaLogin mapear(ResultSet resultSet) throws SQLException {
        OffsetDateTime bloqueadoAte = resultSet.getObject("bloqueado_ate", OffsetDateTime.class);
        return new EstadoTentativaLogin(
                resultSet.getInt("quantidade"),
                resultSet.getObject("ultima_tentativa", OffsetDateTime.class).toInstant(),
                bloqueadoAte == null ? null : bloqueadoAte.toInstant());
    }

    private OffsetDateTime utc(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    public record EstadoTentativaLogin(
            int quantidade,
            Instant ultimaTentativa,
            Instant bloqueadoAte) {
    }
}
