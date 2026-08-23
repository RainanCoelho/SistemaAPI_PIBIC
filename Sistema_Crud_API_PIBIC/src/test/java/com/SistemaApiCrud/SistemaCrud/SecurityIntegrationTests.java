package com.SistemaApiCrud.SistemaCrud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.SistemaApiCrud.SistemaCrud.dto.AlternativaGeradaIaDTO;
import com.SistemaApiCrud.SistemaCrud.dto.PerguntaGeradaIaDTO;
import com.SistemaApiCrud.SistemaCrud.dto.PerguntasGeradasIaDTO;
import com.SistemaApiCrud.SistemaCrud.entity.AlternativaPergunta;
import com.SistemaApiCrud.SistemaCrud.entity.Professor;
import com.SistemaApiCrud.SistemaCrud.entity.Usuario;
import com.SistemaApiCrud.SistemaCrud.entity.CasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.ConteudoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.enums.NivelDificuldade;
import com.SistemaApiCrud.SistemaCrud.entity.enums.PapelUsuario;
import com.SistemaApiCrud.SistemaCrud.entity.enums.StatusCasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.enums.TipoPergunta;
import com.SistemaApiCrud.SistemaCrud.entity.Paciente;
import com.SistemaApiCrud.SistemaCrud.entity.Pergunta;
import com.SistemaApiCrud.SistemaCrud.entity.enums.EstadoCivil;
import com.SistemaApiCrud.SistemaCrud.entity.enums.Sexo;
import com.SistemaApiCrud.SistemaCrud.repository.AlternativaPerguntaRepository;
import com.SistemaApiCrud.SistemaCrud.repository.CasoClinicoRepository;
import com.SistemaApiCrud.SistemaCrud.repository.ConteudoClinicoRepository;
import com.SistemaApiCrud.SistemaCrud.repository.PacienteRepository;
import com.SistemaApiCrud.SistemaCrud.repository.PerguntaRepository;
import com.SistemaApiCrud.SistemaCrud.repository.ProfessorRepository;
import com.SistemaApiCrud.SistemaCrud.repository.UsuarioRepository;
import com.SistemaApiCrud.SistemaCrud.service.PerguntaAiClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTests {

    private static final String FRONT_ORIGIN = "http://localhost:5173";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private CasoClinicoRepository casoRepository;

    @Autowired
    private PerguntaRepository perguntaRepository;

    @Autowired
    private AlternativaPerguntaRepository alternativaRepository;

    @Autowired
    private ConteudoClinicoRepository conteudoRepository;

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private PerguntaAiClient perguntaAiClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deveResponderPreflightCorsParaFrontLocal() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header(HttpHeaders.ORIGIN, FRONT_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(
                                HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                "content-type,x-correlation-id"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT_ORIGIN))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
                .andExpect(resultado -> assertThat(resultado.getResponse().getHeader(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS))
                        .containsIgnoringCase("x-correlation-id"))
                .andExpect(resultado -> assertThat(resultado.getResponse().getHeader(
                        HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS))
                        .containsIgnoringCase(CORRELATION_ID_HEADER));
    }

    @Test
    void devePreservarCabecalhoCorsAoRejeitarCorpoExcedido() throws Exception {
        String corpoExcedido = "{\"username\":\""
                + "a".repeat(1_048_576)
                + "\",\"password\":\"senha\"}";

        mockMvc.perform(post("/auth/login")
                        .header(HttpHeaders.ORIGIN, FRONT_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoExcedido))
                .andExpect(status().is(413))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        FRONT_ORIGIN))
                .andExpect(header().exists(CORRELATION_ID_HEADER))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(413))
                .andExpect(jsonPath("$.type").value(
                        "urn:sistema-api-pibic:problem:corpo-requisicao-excedido"))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.correlationId").isNotEmpty())
                .andExpect(jsonPath("$.erro").value(
                        "O corpo da requisicao excede o limite permitido"));
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
    void professorDeveGerarEPersistirPerguntasComIaNoProprioCaso() throws Exception {
        Map<String, Object> login = login("professor", "professor123");
        String token = (String) login.get("token");
        Professor professor = usuarioRepository.findByUsername("professor").orElseThrow().getProfessor();

        CasoClinico caso = new CasoClinico();
        caso.setProfessor(professor);
        caso.setTitulo("Caso para gerar perguntas");
        caso.setNivelDificuldade(NivelDificuldade.MEDIA);
        caso.setDisciplina("Clinica");
        caso.setAreaSaude("Medicina");
        caso.setEstilo("Multipla escolha");
        caso.setEspecialidade("Pneumologia");
        caso.setStatus(StatusCasoClinico.RASCUNHO);
        caso = casoRepository.save(caso);

        ConteudoClinico conteudo = new ConteudoClinico();
        conteudo.setCasoClinico(caso);
        conteudo.setSintomas("Tosse e febre");
        conteudo.setContexto("Atendimento na emergencia");
        conteudo.setExamClinico("Crepitacoes pulmonares");
        conteudo.setAntecClinico("Sem comorbidades");
        conteudo.setDiagEsperado("Pneumonia comunitaria");
        conteudoRepository.save(conteudo);

        when(perguntaAiClient.gerarPerguntas(any(), any()))
                .thenReturn(new PerguntasGeradasIaDTO(java.util.List.of(
                        new PerguntaGeradaIaDTO(
                                "Qual e a melhor conduta inicial?",
                                "Iniciar o tratamento adequado.",
                                "A",
                                java.util.List.of(
                                        new AlternativaGeradaIaDTO(
                                                "A",
                                                "Iniciar o tratamento indicado",
                                                true),
                                        new AlternativaGeradaIaDTO(
                                                "B",
                                                "Ignorar os sinais clinicos",
                                                false))))));

        mockMvc.perform(post("/casos/" + caso.getIdCaso() + "/ia/perguntas/gerar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantidade": 1,
                                  "tipo": "MULTIPLA_ESCOLHA",
                                  "quantidadeAlternativas": 2,
                                  "dadosSinteticosOuDesidentificados": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].texto").value("Qual e a melhor conduta inicial?"))
                .andExpect(jsonPath("$[0].alternativas.length()").value(2));

        assertThat(perguntaRepository.findByCasoClinicoIdCaso(caso.getIdCaso()))
                .singleElement()
                .extracting(Pergunta::getTexto)
                .isEqualTo("Qual e a melhor conduta inicial?");
    }

    @Test
    void professorDeveGerarDistribuicaoVariadaMesmoComCamposLegadosNulos() throws Exception {
        Map<String, Object> login = login("professor", "professor123");
        String token = (String) login.get("token");
        Professor professor = usuarioRepository.findByUsername("professor").orElseThrow().getProfessor();

        CasoClinico caso = new CasoClinico();
        caso.setProfessor(professor);
        caso.setTitulo("Caso para perguntas variadas");
        caso.setNivelDificuldade(NivelDificuldade.MEDIA);
        caso.setDisciplina("Clinica");
        caso.setAreaSaude("Medicina");
        caso.setEstilo("Perguntas variadas");
        caso.setEspecialidade("Pneumologia");
        caso.setStatus(StatusCasoClinico.RASCUNHO);
        caso = casoRepository.save(caso);

        ConteudoClinico conteudo = new ConteudoClinico();
        conteudo.setCasoClinico(caso);
        conteudo.setSintomas("Tosse, febre e dispneia");
        conteudo.setContexto("Atendimento na emergencia");
        conteudo.setExamClinico("Crepitacoes pulmonares");
        conteudo.setAntecClinico("Sem comorbidades");
        conteudo.setDiagEsperado("Pneumonia comunitaria");
        conteudoRepository.save(conteudo);

        when(perguntaAiClient.gerarPerguntas(any(), any()))
                .thenReturn(new PerguntasGeradasIaDTO(java.util.List.of(
                        new PerguntaGeradaIaDTO(
                                TipoPergunta.MULTIPLA_ESCOLHA,
                                "Qual achado sustenta o diagnostico?",
                                "A febre e as crepitacoes sustentam infeccao pulmonar.",
                                "A",
                                java.util.List.of(
                                        new AlternativaGeradaIaDTO("A", "Febre e crepitacoes", true),
                                        new AlternativaGeradaIaDTO("B", "Ausencia de sintomas", false))),
                        new PerguntaGeradaIaDTO(
                                TipoPergunta.DIAGNOSTICO,
                                "Qual e o diagnostico mais provavel?",
                                "O conjunto de sinais e sintomas e compativel com pneumonia.",
                                "Pneumonia comunitaria|Pneumonia adquirida na comunidade",
                                java.util.List.of()))));

        mockMvc.perform(post("/casos/" + caso.getIdCaso() + "/ia/perguntas/gerar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("Idempotency-Key", "11111111-1111-4111-8111-111111111111")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantidade": null,
                                  "tipo": null,
                                  "quantidadeAlternativas": null,
                                  "distribuicao": [
                                    {
                                      "tipo": "MULTIPLA_ESCOLHA",
                                      "quantidade": 1,
                                      "quantidadeAlternativas": 2
                                    },
                                    {
                                      "tipo": "DIAGNOSTICO",
                                      "quantidade": 1
                                    }
                                  ],
                                  "dadosSinteticosOuDesidentificados": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tipo").value("MULTIPLA_ESCOLHA"))
                .andExpect(jsonPath("$[0].alternativas.length()").value(2))
                .andExpect(jsonPath("$[1].tipo").value("DIAGNOSTICO"))
                .andExpect(jsonPath("$[1].alternativas.length()").value(0));

        assertThat(perguntaRepository.findByCasoClinicoIdCaso(caso.getIdCaso()))
                .extracting(Pergunta::getTipo)
                .containsExactlyInAnyOrder(
                        TipoPergunta.MULTIPLA_ESCOLHA,
                        TipoPergunta.DIAGNOSTICO);
    }

    @Test
    void alunoNaoPodeAdministrarPacientesOuConteudos() throws Exception {
        String token = (String) login("aluno", "aluno123").get("token");

        mockMvc.perform(get("/pacientes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(header().exists(CORRELATION_ID_HEADER))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.type").value(
                        "urn:sistema-api-pibic:problem:acesso-negado"))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.correlationId").isNotEmpty());

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
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists(CORRELATION_ID_HEADER))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.type").value(
                        "urn:sistema-api-pibic:problem:nao-autenticado"))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    void devePadronizarErrosDeValidacaoENaoEncontrado() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists(CORRELATION_ID_HEADER))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.type").value(
                        "urn:sistema-api-pibic:problem:dados-invalidos"))
                .andExpect(jsonPath("$.errors.username").isNotEmpty())
                .andExpect(jsonPath("$.errors.password").isNotEmpty())
                .andExpect(jsonPath("$.correlationId").isNotEmpty());

        String token = (String) login("admin", "admin123").get("token");
        mockMvc.perform(get("/usuarios/999999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(header().exists(CORRELATION_ID_HEADER))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.type").value(
                        "urn:sistema-api-pibic:problem:recurso-nao-encontrado"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());

    }

    @Test
    void devePadronizarConflitoDeEstado() throws Exception {
        String token = (String) login("professor", "professor123").get("token");
        Professor professor = usuarioRepository.findByUsername("professor")
                .orElseThrow()
                .getProfessor();

        CasoClinico caso = new CasoClinico();
        caso.setProfessor(professor);
        caso.setTitulo("Caso publicado para conflito");
        caso.setNivelDificuldade(NivelDificuldade.MEDIA);
        caso.setDisciplina("Clinica");
        caso.setAreaSaude("Medicina");
        caso.setEstilo("Discursiva");
        caso.setEspecialidade("Clinica medica");
        caso.setStatus(StatusCasoClinico.PUBLICADO);
        caso = casoRepository.saveAndFlush(caso);

        mockMvc.perform(post("/perguntas/caso/" + caso.getIdCaso())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "texto": "Qual e o diagnostico?",
                                  "resposta": "Resposta discursiva",
                                  "tipo": "DISCURSIVA",
                                  "gabarito": "REVISAO_MANUAL"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(header().exists(CORRELATION_ID_HEADER))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.type").value(
                        "urn:sistema-api-pibic:problem:conflito-de-estado"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    void professorDeveArquivarCasoPublicadoDeFormaIdempotente() throws Exception {
        String tokenProfessor = (String) login("professor", "professor123").get("token");
        Professor professor = usuarioRepository.findByUsername("professor")
                .orElseThrow()
                .getProfessor();

        CasoClinico publicado = new CasoClinico();
        publicado.setProfessor(professor);
        publicado.setTitulo("Caso a ser arquivado");
        publicado.setNivelDificuldade(NivelDificuldade.MEDIA);
        publicado.setDisciplina("Clinica");
        publicado.setAreaSaude("Medicina");
        publicado.setEstilo("Raciocinio clinico");
        publicado.setEspecialidade("Clinica medica");
        publicado.setStatus(StatusCasoClinico.PUBLICADO);
        publicado.setTempoLimiteMinutos(60);
        publicado = casoRepository.saveAndFlush(publicado);

        for (int tentativa = 0; tentativa < 2; tentativa++) {
            mockMvc.perform(patch("/casos/" + publicado.getIdCaso() + "/arquivar")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProfessor))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ARQUIVADO"));
        }

        Map<String, Object> loginAluno = login("aluno", "aluno123");
        Long idAluno = ((Number) loginAluno.get("idAluno")).longValue();
        String tokenAluno = (String) loginAluno.get("token");
        mockMvc.perform(get("/alunos/" + idAluno + "/casos/"
                        + publicado.getIdCaso() + "/completo")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAluno))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(
                        "O caso clinico ainda nao esta publicado"));

        CasoClinico rascunho = new CasoClinico();
        rascunho.setProfessor(professor);
        rascunho.setTitulo("Rascunho nao arquivavel");
        rascunho.setNivelDificuldade(NivelDificuldade.BAIXA);
        rascunho.setDisciplina("Clinica");
        rascunho.setAreaSaude("Medicina");
        rascunho.setEstilo("Raciocinio clinico");
        rascunho.setEspecialidade("Clinica medica");
        rascunho.setStatus(StatusCasoClinico.RASCUNHO);
        rascunho.setTempoLimiteMinutos(30);
        rascunho = casoRepository.saveAndFlush(rascunho);

        mockMvc.perform(patch("/casos/" + rascunho.getIdCaso() + "/arquivar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProfessor))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(
                        "Publique o caso clinico antes de arquiva-lo"));
    }

    @Test
    void casoDoAlunoNaoDeveExporGabaritoNemDiagnosticoEsperado() throws Exception {
        Professor professor = usuarioRepository.findByUsername("professor").orElseThrow().getProfessor();

        CasoClinico caso = new CasoClinico();
        caso.setProfessor(professor);
        caso.setTitulo("Caso sem vazamento");
        caso.setNivelDificuldade(NivelDificuldade.MEDIA);
        caso.setDisciplina("Clinica");
        caso.setAreaSaude("Medicina");
        caso.setEstilo("Multipla escolha");
        caso.setEspecialidade("Pneumologia");
        caso.setStatus(StatusCasoClinico.PUBLICADO);
        caso = casoRepository.save(caso);

        ConteudoClinico conteudo = new ConteudoClinico();
        conteudo.setCasoClinico(caso);
        conteudo.setSintomas("Tosse");
        conteudo.setContexto("Contexto detalhado");
        conteudo.setExamClinico("Exame detalhado");
        conteudo.setAntecClinico("Sem antecedentes");
        conteudo.setDiagEsperado("Diagnostico secreto");
        conteudoRepository.save(conteudo);

        Pergunta pergunta = new Pergunta();
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

        CasoClinico caso = new CasoClinico();
        caso.setProfessor(outroProfessor);
        caso.setTitulo("Caso de outro professor");
        caso.setNivelDificuldade(NivelDificuldade.MEDIA);
        caso.setDisciplina("Cirurgia");
        caso.setAreaSaude("Medicina");
        caso.setEstilo("Discursiva");
        caso.setEspecialidade("Cirurgia");
        caso.setStatus(StatusCasoClinico.RASCUNHO);
        caso = casoRepository.save(caso);

        Paciente paciente = pacienteRepository.save(new Paciente(
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

        mockMvc.perform(get("/casos/" + caso.getIdCaso() + "/respostas/pendentes-revisao")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/casos/" + caso.getIdCaso() + "/respostas/1/revisao")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correta\":true,\"justificativa\":\"Revisao administrativa\"}"))
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
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(header().exists(CORRELATION_ID_HEADER))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.type").value(
                        "urn:sistema-api-pibic:problem:muitas-tentativas-login"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
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
