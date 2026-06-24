package com.SistemaApiCrud.SistemaCrud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.SistemaApiCrud.SistemaCrud.DTO.responder_caso_request_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.caso_clinico_response_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.resposta_pergunta_request_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.resultado_caso_DTO;
import com.SistemaApiCrud.SistemaCrud.entity.AlternativaPergunta;
import com.SistemaApiCrud.SistemaCrud.entity.Aluno;
import com.SistemaApiCrud.SistemaCrud.entity.Professor;
import com.SistemaApiCrud.SistemaCrud.entity.Usuario;
import com.SistemaApiCrud.SistemaCrud.entity.casos_clinicos;
import com.SistemaApiCrud.SistemaCrud.entity.enums.PapelUsuario;
import com.SistemaApiCrud.SistemaCrud.entity.enums.StatusCasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.enums.TipoPergunta;
import com.SistemaApiCrud.SistemaCrud.entity.pergunta;
import com.SistemaApiCrud.SistemaCrud.exception.BusinessException;
import com.SistemaApiCrud.SistemaCrud.repository.alternativa_pergunta_repository;
import com.SistemaApiCrud.SistemaCrud.repository.aluno_repository;
import com.SistemaApiCrud.SistemaCrud.repository.caso_clinico_repository;
import com.SistemaApiCrud.SistemaCrud.repository.pergunta_repository;
import com.SistemaApiCrud.SistemaCrud.repository.professor_repository;
import com.SistemaApiCrud.SistemaCrud.repository.usuario_repository;
import com.SistemaApiCrud.SistemaCrud.repository.tentativa_caso_repository;
import com.SistemaApiCrud.SistemaCrud.service.caso_clinico_service;
import com.SistemaApiCrud.SistemaCrud.service.JwtService;
import com.SistemaApiCrud.SistemaCrud.service.resposta_aluno_service;

@SpringBootTest
class RespostaAlunoServiceTests {

    @Autowired
    private resposta_aluno_service respostaService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private caso_clinico_service casoService;

    @Autowired
    private aluno_repository alunoRepository;

    @Autowired
    private professor_repository professorRepository;

    @Autowired
    private caso_clinico_repository casoRepository;

    @Autowired
    private pergunta_repository perguntaRepository;

    @Autowired
    private alternativa_pergunta_repository alternativaRepository;

    @Autowired
    private usuario_repository usuarioRepository;

    @Autowired
    private tentativa_caso_repository tentativaRepository;

    @Test
    void deveResponderCasoPublicadoECalcularResultado() {
        Aluno aluno = alunoRepository.save(new Aluno(null, "Ana", "ana@email.com", "Medicina", "4"));
        casos_clinicos caso = criarCaso(StatusCasoClinico.PUBLICADO);
        pergunta pergunta = criarPergunta(caso, "A");

        responder_caso_request_DTO request = new responder_caso_request_DTO(List.of(
                new resposta_pergunta_request_DTO(pergunta.getId(), "A")));

        iniciarTentativa(aluno, caso);
        resultado_caso_DTO resultado = respostaService.responderCaso(aluno.getIdAluno(), caso.getIdCaso(), request);

        assertThat(resultado.getTotalRespondidas()).isEqualTo(1);
        assertThat(resultado.getTotalCorretas()).isEqualTo(1);
        assertThat(resultado.getNota()).isEqualTo(100.0);
        assertThat(resultado.getRespostas()).hasSize(1);
        assertThat(resultado.getRespostas().get(0).getCorreta()).isTrue();

        assertThatThrownBy(() -> respostaService.responderCaso(aluno.getIdAluno(), caso.getIdCaso(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O aluno ja respondeu este caso clinico");
    }

    @Test
    void naoDeveResponderCasoNaoPublicado() {
        Aluno aluno = alunoRepository.save(new Aluno(null, "Bruno", "bruno@email.com", "Medicina", "5"));
        casos_clinicos caso = criarCaso(StatusCasoClinico.RASCUNHO);
        pergunta pergunta = criarPergunta(caso, "B");

        responder_caso_request_DTO request = new responder_caso_request_DTO(List.of(
                new resposta_pergunta_request_DTO(pergunta.getId(), "B")));

        assertThatThrownBy(() -> respostaService.responderCaso(aluno.getIdAluno(), caso.getIdCaso(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O caso clinico ainda nao esta publicado");
    }

    @Test
    void deveExigirTodasAsPerguntasSemDuplicidade() {
        Aluno aluno = alunoRepository.save(new Aluno(null, "Diego", "diego@email.com", "Medicina", "7"));
        casos_clinicos caso = criarCaso(StatusCasoClinico.PUBLICADO);
        pergunta primeira = criarPergunta(caso, "A");
        pergunta segunda = criarPergunta(caso, "B");

        responder_caso_request_DTO incompleta = new responder_caso_request_DTO(List.of(
                new resposta_pergunta_request_DTO(primeira.getId(), "A")));

        iniciarTentativa(aluno, caso);
        assertThatThrownBy(() -> respostaService.responderCaso(aluno.getIdAluno(), caso.getIdCaso(), incompleta))
                .isInstanceOf(com.SistemaApiCrud.SistemaCrud.exception.BadRequestException.class)
                .hasMessage("Todas as perguntas do caso devem ser respondidas exatamente uma vez");

        responder_caso_request_DTO duplicada = new responder_caso_request_DTO(List.of(
                new resposta_pergunta_request_DTO(primeira.getId(), "A"),
                new resposta_pergunta_request_DTO(primeira.getId(), "A")));

        assertThatThrownBy(() -> respostaService.responderCaso(aluno.getIdAluno(), caso.getIdCaso(), duplicada))
                .isInstanceOf(com.SistemaApiCrud.SistemaCrud.exception.BadRequestException.class)
                .hasMessage("Cada pergunta deve ser respondida uma unica vez");

        assertThat(segunda.getId()).isNotNull();
    }

    @Test
    void deveCorrigirRespostaUsandoAlternativasSeparadas() {
        Aluno aluno = alunoRepository.save(new Aluno(null, "Clara", "clara@email.com", "Medicina", "6"));
        casos_clinicos caso = criarCaso(StatusCasoClinico.PUBLICADO);
        pergunta pergunta = criarPergunta(caso, "A");

        alternativaRepository.saveAll(List.of(
                new AlternativaPergunta(null, pergunta, "A", "Conduta antiga", false),
                new AlternativaPergunta(null, pergunta, "B", "Conduta correta", true)));

        responder_caso_request_DTO request = new responder_caso_request_DTO(List.of(
                new resposta_pergunta_request_DTO(pergunta.getId(), "Conduta correta")));

        iniciarTentativa(aluno, caso);
        resultado_caso_DTO resultado = respostaService.responderCaso(aluno.getIdAluno(), caso.getIdCaso(), request);

        assertThat(resultado.getTotalCorretas()).isEqualTo(1);
        assertThat(resultado.getRespostas().get(0).getCorreta()).isTrue();
    }

    @Test
    void naoDeveExibirCasoCompletoNaoPublicadoParaAluno() {
        casos_clinicos caso = criarCaso(StatusCasoClinico.RASCUNHO);

        assertThatThrownBy(() -> casoService.buscarCompletoPublicadoPorId(caso.getIdCaso(), 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O caso clinico ainda nao esta publicado");
    }

    @Test
    void naoDeveAceitarRespostasDepoisDoPrazoDoCaso() {
        Aluno aluno = alunoRepository.save(new Aluno(null, "Elisa", "elisa@email.com", "Medicina", "8"));
        casos_clinicos caso = criarCaso(StatusCasoClinico.PUBLICADO);
        pergunta pergunta = criarPergunta(caso, "A");
        iniciarTentativa(aluno, caso);

        var tentativa = tentativaRepository
                .findByAlunoIdAlunoAndCasoClinicoIdCaso(aluno.getIdAluno(), caso.getIdCaso())
                .orElseThrow();
        tentativa.setDataInicio(Instant.now().minusSeconds(120));
        tentativa.setDataLimite(Instant.now().minusSeconds(1));
        tentativaRepository.saveAndFlush(tentativa);

        responder_caso_request_DTO request = new responder_caso_request_DTO(List.of(
                new resposta_pergunta_request_DTO(pergunta.getId(), "A")));

        assertThatThrownBy(() -> respostaService.responderCaso(aluno.getIdAluno(), caso.getIdCaso(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O tempo limite para responder o caso clinico expirou");
    }

    @Test
    void recarregarCasoNaoDeveReiniciarPrazo() {
        Aluno aluno = alunoRepository.save(new Aluno(null, "Fabio", "fabio@email.com", "Medicina", "9"));
        casos_clinicos caso = criarCaso(StatusCasoClinico.PUBLICADO);
        criarPergunta(caso, "A");

        var primeiraAbertura = casoService.buscarCompletoPublicadoPorId(
                caso.getIdCaso(),
                aluno.getIdAluno());
        var segundaAbertura = casoService.buscarCompletoPublicadoPorId(
                caso.getIdCaso(),
                aluno.getIdAluno());

        assertThat(segundaAbertura.getInicioTentativa()).isEqualTo(primeiraAbertura.getInicioTentativa());
        assertThat(segundaAbertura.getPrazoFinal()).isEqualTo(primeiraAbertura.getPrazoFinal());
        assertThat(segundaAbertura.getSegundosRestantes())
                .isLessThanOrEqualTo(primeiraAbertura.getSegundosRestantes());
    }

    @Test
    void deveListarCasosPaginadosComFiltros() {
        casos_clinicos casoPublicado = criarCaso(StatusCasoClinico.PUBLICADO);
        criarCaso(StatusCasoClinico.RASCUNHO);

        Page<caso_clinico_response_DTO> pagina = casoService.listarPaginado(
                StatusCasoClinico.PUBLICADO,
                casoPublicado.getProfessor().getId(),
                "respiratorio",
                PageRequest.of(0, 10));

        assertThat(pagina.getTotalElements()).isEqualTo(1);
        assertThat(pagina.getContent().get(0).getIdCaso()).isEqualTo(casoPublicado.getIdCaso());
    }

    @Test
    void deveGerarTokenJwtValidoComRoles() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "admin",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        String token = jwtService.gerarToken(authentication);

        assertThat(token.split("\\.")).hasSize(3);
        assertThat(jwtService.isTokenValido(token)).isTrue();
        assertThat(jwtService.criarAuthentication(token).getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void deveAutenticarUsuarioDoBancoComSenhaBCrypt() {
        Usuario usuario = usuarioRepository.findByUsername("admin").orElseThrow();

        assertThat(usuario.getSenha()).isNotEqualTo("admin123");
        assertThat(passwordEncoder.matches("admin123", usuario.getSenha())).isTrue();

        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("admin", "admin123"));

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN");
    }

    @Test
    void deveSalvarUsuarioComRoleESenhaCriptografada() {
        Usuario usuario = new Usuario();
        usuario.setUsername("novo-admin");
        usuario.setSenha(passwordEncoder.encode("senha123"));
        usuario.setRole(PapelUsuario.ADMIN);
        usuario.setAtivo(true);

        Usuario usuarioSalvo = usuarioRepository.save(usuario);

        assertThat(usuarioSalvo.getRole()).isEqualTo(PapelUsuario.ADMIN);
        assertThat(usuarioSalvo.getSenha()).isNotEqualTo("senha123");
        assertThat(passwordEncoder.matches("senha123", usuarioSalvo.getSenha())).isTrue();
    }

    private casos_clinicos criarCaso(StatusCasoClinico status) {
        Professor professor = professorRepository.save(new Professor(null, "Dr. Silva", "silva@email.com", "Clinica"));

        casos_clinicos caso = new casos_clinicos();
        caso.setProfessor(professor);
        caso.setTitulo("Caso respiratorio");
        caso.setDificuldade("MEDIA");
        caso.setDisciplina("Clinica Medica");
        caso.setAreaSaude("Medicina");
        caso.setEstilo("Multipla escolha");
        caso.setEspecialidade("Pneumologia");
        caso.setStatus(status);
        caso.setTempoLimiteMinutos(60);

        return casoRepository.save(caso);
    }

    private pergunta criarPergunta(casos_clinicos caso, String gabarito) {
        pergunta pergunta = new pergunta();
        pergunta.setCasoClinico(caso);
        pergunta.setTexto("Qual a melhor conduta?");
        pergunta.setAlternativaA("A");
        pergunta.setAlternativaB("B");
        pergunta.setAlternativaC("C");
        pergunta.setAlternativaD("D");
        pergunta.setAlternativaE("E");
        pergunta.setResposta(gabarito);
        pergunta.setTipo(TipoPergunta.MULTIPLA_ESCOLHA);
        pergunta.setGabarito(gabarito);

        return perguntaRepository.save(pergunta);
    }

    private void iniciarTentativa(Aluno aluno, casos_clinicos caso) {
        casoService.buscarCompletoPublicadoPorId(caso.getIdCaso(), aluno.getIdAluno());
    }
}
