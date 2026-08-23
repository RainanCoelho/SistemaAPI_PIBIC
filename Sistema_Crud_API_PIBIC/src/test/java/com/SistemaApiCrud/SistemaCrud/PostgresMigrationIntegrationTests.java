package com.SistemaApiCrud.SistemaCrud;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.SistemaApiCrud.SistemaCrud.entity.SolicitacaoGeracaoIa;
import com.SistemaApiCrud.SistemaCrud.entity.Usuario;
import com.SistemaApiCrud.SistemaCrud.entity.enums.EstadoSolicitacaoGeracaoIa;
import com.SistemaApiCrud.SistemaCrud.entity.enums.OperacaoGeracaoIa;
import com.SistemaApiCrud.SistemaCrud.entity.enums.PapelUsuario;
import com.SistemaApiCrud.SistemaCrud.repository.SolicitacaoGeracaoIaRepository;
import com.SistemaApiCrud.SistemaCrud.repository.UsuarioRepository;
import com.SistemaApiCrud.SistemaCrud.service.IdempotenciaGeracaoIaStore;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
class PostgresMigrationIntegrationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private IdempotenciaGeracaoIaStore idempotenciaStore;

    @Autowired
    private SolicitacaoGeracaoIaRepository solicitacaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

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
        List<String> tabelas = jdbcTemplate.queryForList(
                "select tablename from pg_tables where schemaname = 'public'",
                String.class);
        List<String> colunasPergunta = jdbcTemplate.queryForList(
                """
                select column_name from information_schema.columns
                where table_schema = 'public' and table_name = 'pergunta'
                """,
                String.class);
        List<String> colunasCaso = jdbcTemplate.queryForList(
                """
                select column_name from information_schema.columns
                where table_schema = 'public' and table_name = 'casos_clinicos'
                """,
                String.class);
        List<String> colunasIdempotencia = jdbcTemplate.queryForList(
                """
                select column_name from information_schema.columns
                where table_schema = 'public' and table_name = 'solicitacao_geracao_ia'
                """,
                String.class);
        String acaoExclusaoDaChaveCaso = jdbcTemplate.queryForObject(
                """
                select delete_rule from information_schema.referential_constraints
                where constraint_schema = 'public'
                  and constraint_name = 'fk_solicitacao_ia_caso'
                """,
                String.class);

        assertThat(banco)
                .startsWith("sistemacrud_")
                .endsWith("test");
        assertThat(versao).isEqualTo("19");
        assertThat(indices).contains(
                "idx_alternativa_pergunta_pergunta_letra",
                "idx_caso_professor_data",
                "idx_caso_status_data",
                "idx_conteudo_caso_recente",
                "idx_paciente_caso_id",
                "idx_pergunta_caso_id",
                "idx_resposta_caso",
                "idx_resposta_aluno_data",
                "idx_revisao_resposta_data",
                "idx_auditoria_ia_caso_data",
                "idx_tentativa_login_expiracao",
                "idx_cota_uso_ia_atualizacao",
                "idx_lease_uso_ia_expiracao",
                "idx_solicitacao_ia_expiracao");
        assertThat(tabelas).contains(
                "revisao_resposta_aluno",
                "auditoria_geracao_ia",
                "tentativa_login",
                "cota_uso_ia",
                "lease_uso_ia");
        assertThat(indices).doesNotContain(
                "idx_conteudo_caso",
                "idx_resposta_aluno_caso",
                "idx_tentativa_caso_aluno");
        assertThat(colunasPergunta).doesNotContain(
                "alternativa_a",
                "alternativa_b",
                "alternativa_c",
                "alternativa_d",
                "alternativa_e");
        assertThat(colunasCaso)
                .contains("nivel_dificuldade")
                .doesNotContain("dificuldade");
        assertThat(colunasIdempotencia)
                .contains("fk_id_caso", "ids_resultado", "expira_em")
                .doesNotContain("resposta_serializada");
        assertThat(acaoExclusaoDaChaveCaso).isEqualTo("SET NULL");
    }

    @Test
    void deveSerializarReusoConcorrenteDaChaveExpiradaNoPostgres() throws Exception {
        String sufixo = UUID.randomUUID().toString();
        Usuario usuario = usuarioRepository.saveAndFlush(new Usuario(
                null,
                "idempotencia-postgres-" + sufixo,
                "senha",
                PapelUsuario.PROFESSOR,
                true,
                0L,
                null,
                null));
        SolicitacaoGeracaoIa solicitacao = new SolicitacaoGeracaoIa();
        solicitacao.setUsuario(usuario);
        solicitacao.setChaveIdempotencia(UUID.randomUUID().toString());
        solicitacao.setHashRequisicao("a".repeat(64));
        solicitacao.setOperacao(OperacaoGeracaoIa.GERAR_PERGUNTAS);
        solicitacao.setEstado(EstadoSolicitacaoGeracaoIa.CONCLUIDA);
        solicitacao.setStatusResposta(201);
        solicitacao.setIdsResultado("1");
        solicitacao.setCriadoEm(Instant.now().minusSeconds(7200));
        solicitacao.setAtualizadoEm(Instant.now().minusSeconds(3600));
        solicitacao.setExpiraEm(Instant.now().minusSeconds(1));
        solicitacaoRepository.saveAndFlush(solicitacao);

        CountDownLatch partida = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> resultados = List.of(
                    executor.submit(() -> iniciarAposPartida(partida, usuario, solicitacao.getChaveIdempotencia())),
                    executor.submit(() -> iniciarAposPartida(partida, usuario, solicitacao.getChaveIdempotencia())));
            partida.countDown();

            assertThat(resultados.stream().filter(this::obter).count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean iniciarAposPartida(
            CountDownLatch partida,
            Usuario usuario,
            String chave) throws InterruptedException {
        partida.await();
        return idempotenciaStore.iniciar(
                usuario,
                chave,
                "b".repeat(64),
                OperacaoGeracaoIa.GERAR_PERGUNTAS,
                null).criada();
    }

    private boolean obter(Future<Boolean> resultado) {
        try {
            return resultado.get();
        } catch (Exception ex) {
            throw new AssertionError("A reinicializacao concorrente falhou no PostgreSQL", ex);
        }
    }

    private static String valorDoAmbiente(String nome, String valorPadrao) {
        String valor = System.getenv(nome);
        return valor == null || valor.isBlank() ? valorPadrao : valor;
    }
}
