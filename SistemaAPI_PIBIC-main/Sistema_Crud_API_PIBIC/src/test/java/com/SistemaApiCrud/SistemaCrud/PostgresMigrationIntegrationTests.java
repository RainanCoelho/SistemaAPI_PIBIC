package com.SistemaApiCrud.SistemaCrud;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:tc:postgresql:16:///sistemacrud_test",
        "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
        "spring.datasource.username=test",
        "spring.datasource.password=test",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
class PostgresMigrationIntegrationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deveAplicarMigrationsEValidarSchemaNoPostgresReal() {
        String banco = jdbcTemplate.queryForObject(
                "select current_database()",
                String.class);
        String versao = jdbcTemplate.queryForObject(
                "select version from flyway_schema_history where success = true order by installed_rank desc limit 1",
                String.class);

        assertThat(banco).isEqualTo("sistemacrud_test");
        assertThat(versao).isEqualTo("6");
    }
}
