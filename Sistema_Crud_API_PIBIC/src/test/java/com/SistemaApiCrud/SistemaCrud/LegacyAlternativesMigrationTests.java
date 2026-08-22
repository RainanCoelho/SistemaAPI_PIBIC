package com.SistemaApiCrud.SistemaCrud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.zaxxer.hikari.HikariDataSource;

class LegacyAlternativesMigrationTests {

    @Test
    void deveMigrarAlternativasLegadasSemSobrescreverRegistrosNormalizados()
            throws Exception {
        HikariDataSource dataSource = novoBanco();
        try (dataSource) {
            migrarAteVersao12(dataSource);
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            inserirCasoEProfessor(jdbc);
            inserirPerguntaLegada(jdbc, "Opcao antiga A", "Opcao antiga B");
            jdbc.update("""
                    INSERT INTO alternativa_pergunta
                        (fk_id_pergunta, letra, texto, correta)
                    VALUES (300, 'B', 'Opcao B ja normalizada', FALSE)
                    """);

            migrarAteUltimaVersao(dataSource);

            List<Map<String, Object>> alternativas = jdbc.queryForList("""
                    SELECT letra, texto, correta
                    FROM alternativa_pergunta
                    WHERE fk_id_pergunta = 300
                    ORDER BY letra
                    """);
            assertThat(alternativas).hasSize(2);
            assertThat(alternativas.get(0))
                    .containsEntry("LETRA", "A")
                    .containsEntry("TEXTO", "Opcao antiga A")
                    .containsEntry("CORRETA", true);
            assertThat(alternativas.get(1))
                    .containsEntry("LETRA", "B")
                    .containsEntry("TEXTO", "Opcao B ja normalizada")
                    .containsEntry("CORRETA", false);
            assertThat(ultimaVersao(jdbc)).isEqualTo("13");
        }
    }

    @Test
    void deveInterromperMigracaoQuandoPerguntaLegadaNaoPodeSerNormalizada()
            throws Exception {
        HikariDataSource dataSource = novoBanco();
        try (dataSource) {
            migrarAteVersao12(dataSource);
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            inserirCasoEProfessor(jdbc);
            inserirPerguntaLegada(jdbc, "Unica opcao", null);

            assertThatThrownBy(() -> migrarAteUltimaVersao(dataSource))
                    .isInstanceOf(FlywayException.class)
                    .hasMessageContaining("V13__migrar_alternativas_legadas.sql");
        }
    }

    private HikariDataSource novoBanco() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:h2:mem:migracao_"
                + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setMaximumPoolSize(3);
        dataSource.setMinimumIdle(1);
        return dataSource;
    }

    private void migrarAteVersao12(DataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("12")
                .load()
                .migrate();
    }

    private void migrarAteUltimaVersao(DataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    private void inserirCasoEProfessor(JdbcTemplate jdbc) {
        jdbc.update("""
                INSERT INTO professor (id, nome, email, materia)
                VALUES (100, 'Professora Migracao', 'migracao@example.com', 'Clinica')
                """);
        jdbc.update("""
                INSERT INTO casos_clinicos (
                    id_caso,
                    fk_id_professor,
                    titulo,
                    dificuldade,
                    disciplina,
                    area_saude,
                    estilo,
                    especialidade,
                    status,
                    data_criacao,
                    data_atualizacao,
                    nivel_dificuldade,
                    tempo_limite_minutos
                ) VALUES (
                    200,
                    100,
                    'Caso para migracao',
                    'MEDIA',
                    'Clinica',
                    'Medicina',
                    'Multipla escolha',
                    'Clinica medica',
                    'RASCUNHO',
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP,
                    'MEDIA',
                    60
                )
                """);
    }

    private void inserirPerguntaLegada(
            JdbcTemplate jdbc,
            String alternativaA,
            String alternativaB) {
        jdbc.update("""
                INSERT INTO pergunta (
                    id,
                    fk_id_caso,
                    texto,
                    alternativa_a,
                    alternativa_b,
                    resposta,
                    tipo,
                    gabarito
                ) VALUES (?, 200, 'Pergunta legada', ?, ?, ?, 'MULTIPLA_ESCOLHA', ?)
                """,
                300,
                alternativaA,
                alternativaB,
                alternativaA,
                alternativaA);
    }

    private String ultimaVersao(JdbcTemplate jdbc) {
        return jdbc.queryForObject("""
                SELECT "version"
                FROM "flyway_schema_history"
                WHERE "success" = TRUE
                ORDER BY "installed_rank" DESC
                LIMIT 1
                """, String.class);
    }
}
