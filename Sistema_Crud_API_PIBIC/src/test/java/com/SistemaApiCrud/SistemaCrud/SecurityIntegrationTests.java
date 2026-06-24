package com.SistemaApiCrud.SistemaCrud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.SistemaApiCrud.SistemaCrud.entity.AlternativaPergunta;
import com.SistemaApiCrud.SistemaCrud.entity.Professor;
import com.SistemaApiCrud.SistemaCrud.entity.Usuario;
import com.SistemaApiCrud.SistemaCrud.entity.casos_clinicos;
import com.SistemaApiCrud.SistemaCrud.entity.conteudo_clinico;
import com.SistemaApiCrud.SistemaCrud.entity.enums.PapelUsuario;
import com.SistemaApiCrud.SistemaCrud.entity.enums.StatusCasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.enums.TipoPergunta;
import com.SistemaApiCrud.SistemaCrud.entity.paciente;
import com.SistemaApiCrud.SistemaCrud.entity.pergunta;
import com.SistemaApiCrud.SistemaCrud.entity.enums.EstadoCivil;
import com.SistemaApiCrud.SistemaCrud.entity.enums.Sexo;
import com.SistemaApiCrud.SistemaCrud.repository.alternativa_pergunta_repository;
import com.SistemaApiCrud.SistemaCrud.repository.caso_clinico_repository;
import com.SistemaApiCrud.SistemaCrud.repository.conteudo_clinico_repository;
import com.SistemaApiCrud.SistemaCrud.repository.paciente_repository;
import com.SistemaApiCrud.SistemaCrud.repository.pergunta_repository;
import com.SistemaApiCrud.SistemaCrud.repository.professor_repository;
import com.SistemaApiCrud.SistemaCrud.repository.usuario_repository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTests {

    private static final String FRONT_ORIGIN = "http://localhost:5173";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private usuario_repository usuarioRepository;

    @Autowired
    private professor_repository professorRepository;

    @Autowired
    private caso_clinico_repository casoRepository;

    @Autowired
    private pergunta_repository perguntaRepository;

    @Autowired
    private alternativa_pergunta_repository alternativaRepository;

    @Autowired
    private conteudo_clinico_repository conteudoRepository;

    @Autowired
    private paciente_repository pacienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deveResponderPreflightCorsParaFrontLocal() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header(HttpHeaders.ORIGIN, FRONT_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT_ORIGIN))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void deveAutenticarComJwtEAcessarRotaProtegida() throws Exception {
        Map<String, Object> json = login("admin", "admin123");
        String token = (String) json.get("token");

        assertThat(token).isNotBlank();

        mockMvc.perform(get("/usuarios")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void devePermitirAlunoAcessarSomenteProprioPerfil() throws Exception {
        Map<String, Object> json = login("aluno", "aluno123");
        String token = (String) json.get("token");
        Long idAluno = ((Number) json.get("idAluno")).longValue();

        mockMvc.perform(get("/alunos/" + idAluno + "/historico")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/alunos/" + (idAluno + 999L) + "/historico")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveCriarCasoNoProfessorAutenticadoEBloquearFiltroDeOutroProfessor() throws Exception {
        Map<String, Object> json = login("professor", "professor123");
        String token = (String) json.get("token");
        Long idProfessor = ((Number) json.get("idProfessor")).longValue();

        String respostaCaso = mockMvc.perform(post("/casos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Caso de seguranca",
                                  "dificuldade": "MEDIA",
                                  "disciplina": "Clinica Medica",
                                  "areaSaude": "Medicina",
                                  "estilo": "Multipla escolha",
                                  "especialidade": "Pneumologia",
                                  "objetivoAprendizagem": "Testar acesso por dono",
                                  "nivelDificuldade": "MEDIA"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> caso = objectMapper.readValue(respostaCaso, new TypeReference<>() {});
        assertThat(((Number) caso.get("idProfessor")).longValue()).isEqualTo(idProfessor);

        mockMvc.perform(get("/casos?idProfessor=" + (idProfessor + 999L))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void alunoNaoPodeAdministrarPacientesOuConteudos() throws Exception {
        String token = (String) login("aluno", "aluno123").get("token");

        mockMvc.perform(get("/pacientes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/conteudos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void cadastroPublicoDeProfessorDeveExigirAdministrador() throws Exception {
        mockMvc.perform(post("/professores/cadastro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Professor Externo",
                                  "email": "externo@example.com",
                                  "materia": "Clinica",
                                  "username": "professor-externo",
                                  "senha": "senha-forte-123"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void casoDoAlunoNaoDeveExporGabaritoNemDiagnosticoEsperado() throws Exception {
        Professor professor = usuarioRepository.findByUsername("professor").orElseThrow().getProfessor();

        casos_clinicos caso = new casos_clinicos();
        caso.setProfessor(professor);
        caso.setTitulo("Caso sem vazamento");
        caso.setDificuldade("MEDIA");
        caso.setDisciplina("Clinica");
        caso.setAreaSaude("Medicina");
        caso.setEstilo("Multipla escolha");
        caso.setEspecialidade("Pneumologia");
        caso.setStatus(StatusCasoClinico.PUBLICADO);
        caso = casoRepository.save(caso);

        conteudo_clinico conteudo = new conteudo_clinico();
        conteudo.setCasoClinico(caso);
        conteudo.setSintomas("Tosse");
        conteudo.setContexto("Contexto detalhado");
        conteudo.setExamClinico("Exame detalhado");
        conteudo.setAntecClinico("Sem antecedentes");
        conteudo.setDiagEsperado("Diagnostico secreto");
        conteudoRepository.save(conteudo);

        pergunta pergunta = new pergunta();
        pergunta.setCasoClinico(caso);
        pergunta.setTexto("Qual a conduta?");
        pergunta.setResposta("A");
        pergunta.setGabarito("A");
        pergunta.setTipo(TipoPergunta.MULTIPLA_ESCOLHA);
        pergunta = perguntaRepository.save(pergunta);
        alternativaRepository.save(new AlternativaPergunta(null, pergunta, "A", "Conduta correta", true));

        Map<String, Object> aluno = login("aluno", "aluno123");
        String token = (String) aluno.get("token");
        Long idAluno = ((Number) aluno.get("idAluno")).longValue();

        mockMvc.perform(get("/alunos/" + idAluno + "/casos/" + caso.getIdCaso() + "/completo")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perguntas[0].gabarito").doesNotExist())
                .andExpect(jsonPath("$.perguntas[0].resposta").doesNotExist())
                .andExpect(jsonPath("$.perguntas[0].alternativas[0].correta").doesNotExist())
                .andExpect(jsonPath("$.conteudosClinicos[0].diagEsperado").doesNotExist())
                .andExpect(jsonPath("$.inicioTentativa").isNotEmpty())
                .andExpect(jsonPath("$.prazoFinal").isNotEmpty())
                .andExpect(jsonPath("$.segundosRestantes").isNumber())
                .andExpect(jsonPath("$.caso.tempoLimiteMinutos").value(60));
    }

    @Test
    void professorNaoPodeAcessarPacienteDeOutroProfessor() throws Exception {
        Professor outroProfessor = professorRepository.save(
                new Professor(null, "Outro Professor", "outro@example.com", "Cirurgia"));

        casos_clinicos caso = new casos_clinicos();
        caso.setProfessor(outroProfessor);
        caso.setTitulo("Caso de outro professor");
        caso.setDificuldade("MEDIA");
        caso.setDisciplina("Cirurgia");
        caso.setAreaSaude("Medicina");
        caso.setEstilo("Discursiva");
        caso.setEspecialidade("Cirurgia");
        caso.setStatus(StatusCasoClinico.RASCUNHO);
        caso = casoRepository.save(caso);

        paciente paciente = pacienteRepository.save(new paciente(
                null,
                caso,
                "Paciente Protegido",
                "Professor",
                Sexo.NAO_INFORMADO,
                40,
                EstadoCivil.NAO_INFORMADO,
                "1,70 m",
                "70 kg"));

        String token = (String) login("professor", "professor123").get("token");

        mockMvc.perform(get("/pacientes/" + paciente.getIdPaciente())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void tokenDeveSerInvalidadoQuandoUsuarioForDesativado() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setUsername("admin-token-revogado");
        usuario.setSenha(passwordEncoder.encode("senha-token-123"));
        usuario.setRole(PapelUsuario.ADMIN);
        usuario.setAtivo(true);
        usuario.setVersaoCredencial(0L);
        usuario = usuarioRepository.saveAndFlush(usuario);

        String token = (String) login(usuario.getUsername(), "senha-token-123").get("token");

        usuario.setAtivo(false);
        usuario.setVersaoCredencial(1L);
        usuarioRepository.saveAndFlush(usuario);

        mockMvc.perform(get("/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveBloquearExcessoDeTentativasDeLogin() throws Exception {
        for (int tentativa = 0; tentativa < 5; tentativa++) {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "conta-inexistente-bloqueio",
                                      "password": "senha-incorreta"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "conta-inexistente-bloqueio",
                                  "password": "senha-incorreta"
                                }
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER));
    }

    private Map<String, Object> login(String username, String password) throws Exception {
        String respostaLogin = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(respostaLogin, new TypeReference<>() {});
    }
}
