package com.SistemaApiCrud.SistemaCrud;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
class PostgresConcorrenciaELoteTests extends ConcorrenciaELoteTests {

    @DynamicPropertySource
    static void banco(DynamicPropertyRegistry registro) {
        PostgresMigrationIntegrationTests.configurarBancoPostgres(registro);
    }
}
