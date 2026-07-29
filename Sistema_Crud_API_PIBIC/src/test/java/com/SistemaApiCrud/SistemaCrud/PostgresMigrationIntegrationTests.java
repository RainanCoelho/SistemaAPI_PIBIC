package com.SistemaApiCrud.SistemaCrud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
class PostgresMigrationIntegrationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configurarBancoPostgres(DynamicPropertyRegistry registro) {
        registro.add(
                "spring.datasource.url",
                () -> valorDoAmbiente(
                        "POSTGRES_TESTE_URL",
                        "jdbc:tc:postgresql:16:///sistemacrud_test"));
        registro.add(
                "spring.datasource.driver-class-name",
                () -> valorDoAmbiente(
                        "POSTGRES_TESTE_DRIVER",
                        "org.testcontainers.jdbc.ContainerDatabaseDriver"));
        registro.add(
                "spring.datasource.username",
                () -> valorDoAmbiente("POSTGRES_TESTE_USUARIO", "test"));
        registro.add(
                "spring.datasource.password",
                () -> valorDoAmbiente("POSTGRES_TESTE_SENHA", "test"));
    }

    @Test
    void deveAplicarMigrationsEValidarSchemaNoPostgresReal() {
        String banco = jdbcTemplate.queryForObject(
                "select current_database()",
                String.class);
        String versao = jdbcTemplate.queryForObject(
                "select version from flyway_schema_history where success = true order by installed_rank desc limit 1",
                String.class);
        List<String> indices = jdbcTemplate.queryForList(
                "select indexname from pg_indexes where schemaname = 'public'",
                String.class);

        assertThat(banco)
                .startsWith("sistemacrud_")
                .endsWith("test");
        assertThat(versao).isEqualTo("8");
        assertThat(indices).contains(
                "idx_alternativa_pergunta_pergunta_letra",
                "idx_conteudo_caso_recente",
                "idx_resposta_caso",
                "idx_resposta_aluno_data");
        assertThat(indices).doesNotContain(
                "idx_conteudo_caso",
                "idx_resposta_aluno_caso",
                "idx_tentativa_caso_aluno");
    }

    private static String valorDoAmbiente(String nome, String valorPadrao) {
        String valor = System.getenv(nome);
        return valor == null || valor.isBlank() ? valorPadrao : valor;
    }
}
