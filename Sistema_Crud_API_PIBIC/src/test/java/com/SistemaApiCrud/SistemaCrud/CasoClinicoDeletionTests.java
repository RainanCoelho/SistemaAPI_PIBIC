package com.SistemaApiCrud.SistemaCrud;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.SistemaApiCrud.SistemaCrud.entity.AlternativaPergunta;
import com.SistemaApiCrud.SistemaCrud.entity.CasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.ConteudoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.Paciente;
import com.SistemaApiCrud.SistemaCrud.entity.Pergunta;
import com.SistemaApiCrud.SistemaCrud.entity.Professor;
import com.SistemaApiCrud.SistemaCrud.entity.enums.EstadoCivil;
import com.SistemaApiCrud.SistemaCrud.entity.enums.NivelDificuldade;
import com.SistemaApiCrud.SistemaCrud.entity.enums.Sexo;
import com.SistemaApiCrud.SistemaCrud.entity.enums.StatusCasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.enums.TipoPergunta;
import com.SistemaApiCrud.SistemaCrud.repository.AlternativaPerguntaRepository;
import com.SistemaApiCrud.SistemaCrud.repository.CasoClinicoRepository;
import com.SistemaApiCrud.SistemaCrud.repository.ConteudoClinicoRepository;
import com.SistemaApiCrud.SistemaCrud.repository.PacienteRepository;
import com.SistemaApiCrud.SistemaCrud.repository.PerguntaRepository;
import com.SistemaApiCrud.SistemaCrud.repository.ProfessorRepository;
import com.SistemaApiCrud.SistemaCrud.service.CasoClinicoService;

@SpringBootTest
class CasoClinicoDeletionTests {

    @Autowired
    private AlternativaPerguntaRepository alternativaRepository;

    @Autowired
    private CasoClinicoRepository casoRepository;

    @Autowired
    private CasoClinicoService casoService;

    @Autowired
    private ConteudoClinicoRepository conteudoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private PerguntaRepository perguntaRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @BeforeEach
    void autenticarAdministrador() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void limparAutenticacao() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveExcluirCasoPublicadoComTodosOsDadosVinculados() {
        String marcador = UUID.randomUUID().toString();
        Professor professor = professorRepository.save(new Professor(
                null,
                "Professor Exclusao " + marcador,
                "exclusao-" + marcador + "@example.com",
                "Clinica"));

        CasoClinico caso = new CasoClinico();
        caso.setProfessor(professor);
        caso.setTitulo("Caso publicado para exclusao");
        caso.setDisciplina("Semiologia");
        caso.setAreaSaude("Medicina");
        caso.setEstilo("Multipla escolha");
        caso.setEspecialidade("Cardiologia");
        caso.setStatus(StatusCasoClinico.PUBLICADO);
        caso.setNivelDificuldade(NivelDificuldade.MEDIA);
        caso.setTempoLimiteMinutos(60);
        caso = casoRepository.saveAndFlush(caso);

        pacienteRepository.saveAndFlush(new Paciente(
                null,
                caso,
                "Paciente simulado",
                "Professor",
                Sexo.NAO_INFORMADO,
                45,
                EstadoCivil.NAO_INFORMADO,
                "170",
                "70 kg"));

        ConteudoClinico conteudo = new ConteudoClinico();
        conteudo.setCasoClinico(caso);
        conteudo.setSintomas("Dor toracica");
        conteudo.setContexto("Pronto atendimento");
        conteudo.setExamClinico("Exame clinico");
        conteudo.setAntecClinico("Sem antecedentes");
        conteudo.setDiagEsperado("Infarto agudo do miocardio");
        conteudoRepository.saveAndFlush(conteudo);

        Pergunta pergunta = new Pergunta();
        pergunta.setCasoClinico(caso);
        pergunta.setTexto("Qual e a conduta inicial?");
        pergunta.setResposta("Monitorizacao");
        pergunta.setTipo(TipoPergunta.MULTIPLA_ESCOLHA);
        pergunta.setGabarito("A");
        pergunta = perguntaRepository.saveAndFlush(pergunta);
        alternativaRepository.saveAndFlush(new AlternativaPergunta(
                null,
                pergunta,
                "A",
                "Monitorizacao",
                true));

        Long idCaso = caso.getIdCaso();
        Long idPergunta = pergunta.getId();
        Long idAluno = inserirAluno(marcador);
        Long idRevisor = inserirRevisor(marcador);
        Long idResposta = inserirResposta(idAluno, idCaso, idPergunta);
        inserirRevisao(idResposta, idRevisor);
        inserirTentativa(idAluno, idCaso);
        inserirAuditoria(idCaso, idRevisor);
        Long idSolicitacao = inserirSolicitacao(idCaso, idRevisor);

        casoService.deletar(idCaso);

        assertThat(casoRepository.existsById(idCaso)).isFalse();
        assertThat(contarPorCaso("paciente", idCaso)).isZero();
        assertThat(contarPorCaso("conteudo_clinico", idCaso)).isZero();
        assertThat(contarPorCaso("pergunta", idCaso)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM alternativa_pergunta WHERE fk_id_pergunta = ?",
                Long.class,
                idPergunta)).isZero();
        assertThat(contarPorCaso("resposta_aluno", idCaso)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM revisao_resposta_aluno WHERE fk_id_resposta = ?",
                Long.class,
                idResposta)).isZero();
        assertThat(contarPorCaso("tentativa_caso", idCaso)).isZero();
        assertThat(contarPorCaso("auditoria_geracao_ia", idCaso)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM solicitacao_geracao_ia WHERE id = ? AND fk_id_caso IS NULL",
                Long.class,
                idSolicitacao)).isOne();
    }

    private Long inserirAluno(String marcador) {
        String email = "aluno-exclusao-" + marcador + "@example.com";
        jdbcTemplate.update(
                "INSERT INTO aluno (nome, email, curso, periodo) VALUES (?, ?, ?, ?)",
                "Aluno Exclusao",
                email,
                "Medicina",
                "5");
        return jdbcTemplate.queryForObject(
                "SELECT id_aluno FROM aluno WHERE email = ?",
                Long.class,
                email);
    }

    private Long inserirRevisor(String marcador) {
        String username = "revisor-" + marcador;
        jdbcTemplate.update(
                "INSERT INTO usuario (username, senha, role, ativo) VALUES (?, ?, ?, TRUE)",
                username,
                "senha-de-teste",
                "ADMIN");
        return jdbcTemplate.queryForObject(
                "SELECT id FROM usuario WHERE username = ?",
                Long.class,
                username);
    }

    private Long inserirResposta(Long idAluno, Long idCaso, Long idPergunta) {
        jdbcTemplate.update(
                """
                INSERT INTO resposta_aluno
                    (fk_id_aluno, fk_id_caso, fk_id_pergunta, resposta_marcada, correta, data_resposta)
                VALUES (?, ?, ?, ?, TRUE, CURRENT_TIMESTAMP)
                """,
                idAluno,
                idCaso,
                idPergunta,
                "A");
        return jdbcTemplate.queryForObject(
                """
                SELECT id FROM resposta_aluno
                WHERE fk_id_aluno = ? AND fk_id_caso = ? AND fk_id_pergunta = ?
                """,
                Long.class,
                idAluno,
                idCaso,
                idPergunta);
    }

    private void inserirRevisao(Long idResposta, Long idRevisor) {
        jdbcTemplate.update(
                """
                INSERT INTO revisao_resposta_aluno
                    (fk_id_resposta, fk_id_revisor, correta, justificativa, data_revisao, versao_revisao)
                VALUES (?, ?, TRUE, ?, CURRENT_TIMESTAMP, 1)
                """,
                idResposta,
                idRevisor,
                "Resposta revisada");
    }

    private void inserirTentativa(Long idAluno, Long idCaso) {
        OffsetDateTime inicio = OffsetDateTime.now();
        jdbcTemplate.update(
                """
                INSERT INTO tentativa_caso
                    (fk_id_aluno, fk_id_caso, data_inicio, data_limite)
                VALUES (?, ?, ?, ?)
                """,
                idAluno,
                idCaso,
                inicio,
                inicio.plusHours(1));
    }

    private void inserirAuditoria(Long idCaso, Long idUsuario) {
        jdbcTemplate.update(
                """
                INSERT INTO auditoria_geracao_ia
                    (fk_id_caso, fk_id_usuario, operacao, provedor, modelo, versao_prompt,
                     hash_contexto, hash_saida, referencia_resultado, quantidade_itens,
                     dados_desidentificados_confirmados, data_geracao)
                VALUES (?, ?, 'GERAR_CASO', 'teste', 'teste', 'v1', ?, ?, ?, 1, TRUE, ?)
                """,
                idCaso,
                idUsuario,
                "0".repeat(64),
                "1".repeat(64),
                "resultado-de-teste",
                OffsetDateTime.now());
    }

    private Long inserirSolicitacao(Long idCaso, Long idUsuario) {
        String chave = UUID.randomUUID().toString();
        jdbcTemplate.update(
                """
                INSERT INTO solicitacao_geracao_ia
                    (fk_id_usuario, fk_id_caso, operacao, chave_idempotencia, hash_requisicao,
                     estado, criado_em, atualizado_em)
                VALUES (?, ?, 'GERAR_CASO', ?, ?, 'EM_ANDAMENTO', ?, ?)
                """,
                idUsuario,
                idCaso,
                chave,
                "2".repeat(64),
                OffsetDateTime.now(),
                OffsetDateTime.now());
        return jdbcTemplate.queryForObject(
                "SELECT id FROM solicitacao_geracao_ia WHERE fk_id_usuario = ? AND chave_idempotencia = ?",
                Long.class,
                idUsuario,
                chave);
    }

    private long contarPorCaso(String tabela, Long idCaso) {
        String sql = "SELECT COUNT(*) FROM " + tabela + " WHERE fk_id_caso = ?";
        return jdbcTemplate.queryForObject(sql, Long.class, idCaso);
    }
}
