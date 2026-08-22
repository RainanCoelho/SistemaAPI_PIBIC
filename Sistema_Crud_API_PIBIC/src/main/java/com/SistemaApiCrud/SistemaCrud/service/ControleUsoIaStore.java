package com.SistemaApiCrud.SistemaCrud.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class ControleUsoIaStore {

    private static final int SLOT_LEASE = 0;
    private static final int TOTAL_SLOTS_COTA = 32;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public ControleUsoIaStore(
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public ResultadoCota registrarUso(
            String identificadorHash,
            Instant inicioMinuto,
            LocalDate dia,
            int limitePorMinuto,
            int limitePorDia) {
        return transactionTemplate.execute(status -> {
            bloquearCoordenacao(slotCota(identificadorHash));
            Optional<EstadoCota> atual = buscarCota(identificadorHash);
            if (atual.isEmpty()) {
                jdbcTemplate.update(
                        """
                        INSERT INTO cota_uso_ia
                            (identificador_hash, minuto_inicio, usos_minuto, dia, usos_dia, atualizado_em)
                        VALUES (?, ?, 1, ?, 1, ?)
                        """,
                        identificadorHash,
                        utc(inicioMinuto),
                        dia,
                        utc(inicioMinuto));
                return ResultadoCota.PERMITIDO;
            }

            EstadoCota estado = atual.get();
            int usosMinuto = estado.inicioMinuto().equals(inicioMinuto)
                    ? estado.usosMinuto()
                    : 0;
            int usosDia = estado.dia().equals(dia) ? estado.usosDia() : 0;
            if (usosMinuto >= limitePorMinuto) {
                return ResultadoCota.LIMITE_MINUTO;
            }
            if (usosDia >= limitePorDia) {
                return ResultadoCota.LIMITE_DIA;
            }

            jdbcTemplate.update(
                    """
                    UPDATE cota_uso_ia
                       SET minuto_inicio = ?, usos_minuto = ?, dia = ?, usos_dia = ?, atualizado_em = ?
                     WHERE identificador_hash = ?
                    """,
                    utc(inicioMinuto),
                    usosMinuto + 1,
                    dia,
                    usosDia + 1,
                    utc(inicioMinuto),
                    identificadorHash);
            return ResultadoCota.PERMITIDO;
        });
    }

    public Optional<String> adquirirLease(
            String identificadorHash,
            Duration duracao,
            int maximoSimultaneas) {
        return transactionTemplate.execute(status -> {
            bloquearCoordenacao(SLOT_LEASE);
            Instant agora = agoraBanco();
            jdbcTemplate.update(
                    "DELETE FROM lease_uso_ia WHERE expira_em <= ?",
                    utc(agora));
            Long quantidade = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM lease_uso_ia",
                    Long.class);
            if (quantidade != null && quantidade >= maximoSimultaneas) {
                return Optional.empty();
            }

            String id = UUID.randomUUID().toString();
            jdbcTemplate.update(
                    """
                    INSERT INTO lease_uso_ia
                        (id, identificador_hash, criado_em, expira_em)
                    VALUES (?, ?, ?, ?)
                    """,
                    id,
                    identificadorHash,
                    utc(agora),
                    utc(agora.plus(duracao)));
            return Optional.of(id);
        });
    }

    public boolean renovarLease(String id, Duration duracao) {
        Boolean renovado = transactionTemplate.execute(status -> {
            bloquearCoordenacao(SLOT_LEASE);
            Instant agora = agoraBanco();
            return jdbcTemplate.update(
                    """
                    UPDATE lease_uso_ia
                       SET expira_em = ?
                     WHERE id = ? AND expira_em > ?
                    """,
                    utc(agora.plus(duracao)),
                    id,
                    utc(agora)) == 1;
        });
        return Boolean.TRUE.equals(renovado);
    }

    public void liberarLease(String id) {
        transactionTemplate.executeWithoutResult(status -> {
            bloquearCoordenacao(SLOT_LEASE);
            jdbcTemplate.update("DELETE FROM lease_uso_ia WHERE id = ?", id);
        });
    }

    public void removerCotasAnterioresA(Instant limite) {
        jdbcTemplate.update(
                "DELETE FROM cota_uso_ia WHERE atualizado_em < ?",
                utc(limite));
    }

    private Optional<EstadoCota> buscarCota(String identificadorHash) {
        List<EstadoCota> resultados = jdbcTemplate.query(
                """
                SELECT minuto_inicio, usos_minuto, dia, usos_dia
                  FROM cota_uso_ia
                 WHERE identificador_hash = ?
                """,
                (resultSet, rowNum) -> mapear(resultSet),
                identificadorHash);
        return resultados.stream().findFirst();
    }

    private void bloquearCoordenacao(int slot) {
        jdbcTemplate.queryForObject(
                "SELECT slot FROM coordenacao_uso_ia WHERE slot = ? FOR UPDATE",
                Integer.class,
                slot);
    }

    private int slotCota(String identificadorHash) {
        return 1 + Math.floorMod(identificadorHash.hashCode(), TOTAL_SLOTS_COTA);
    }

    private Instant agoraBanco() {
        OffsetDateTime agora = jdbcTemplate.queryForObject(
                "SELECT CURRENT_TIMESTAMP",
                OffsetDateTime.class);
        if (agora == null) {
            throw new IllegalStateException("O banco nao informou o horario atual");
        }
        return agora.toInstant();
    }

    private EstadoCota mapear(ResultSet resultSet) throws SQLException {
        return new EstadoCota(
                resultSet.getObject("minuto_inicio", OffsetDateTime.class).toInstant(),
                resultSet.getInt("usos_minuto"),
                resultSet.getObject("dia", LocalDate.class),
                resultSet.getInt("usos_dia"));
    }

    private OffsetDateTime utc(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    public enum ResultadoCota {
        PERMITIDO,
        LIMITE_MINUTO,
        LIMITE_DIA
    }

    private record EstadoCota(
            Instant inicioMinuto,
            int usosMinuto,
            LocalDate dia,
            int usosDia) {
    }
}
