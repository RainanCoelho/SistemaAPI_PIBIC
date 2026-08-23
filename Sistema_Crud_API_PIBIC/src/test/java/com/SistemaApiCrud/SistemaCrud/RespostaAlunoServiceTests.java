package com.SistemaApiCrud.SistemaCrud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.SistemaApiCrud.SistemaCrud.DTO.responder_caso_request_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.alternativa_pergunta_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.caso_clinico_response_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.conteudo_clinico_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.paciente_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.pergunta_request_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.resposta_pergunta_request_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.resultado_caso_DTO;
import com.SistemaApiCrud.SistemaCrud.entity.AlternativaPergunta;
import com.SistemaApiCrud.SistemaCrud.entity.Aluno;
import com.SistemaApiCrud.SistemaCrud.entity.Professor;
import com.SistemaApiCrud.SistemaCrud.entity.Usuario;
import com.SistemaApiCrud.SistemaCrud.entity.casos_clinicos;
import com.SistemaApiCrud.SistemaCrud.entity.conteudo_clinico;
import com.SistemaApiCrud.SistemaCrud.entity.enums.EstadoCivil;
import com.SistemaApiCrud.SistemaCrud.entity.enums.NivelDificuldade;
import com.SistemaApiCrud.SistemaCrud.entity.enums.PapelUsuario;
import com.SistemaApiCrud.SistemaCrud.entity.enums.Sexo;
import com.SistemaApiCrud.SistemaCrud.entity.enums.StatusCasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.enums.TipoPergunta;
import com.SistemaApiCrud.SistemaCrud.entity.paciente;
import com.SistemaApiCrud.SistemaCrud.entity.pergunta;
import com.SistemaApiCrud.SistemaCrud.exception.BusinessException;
import com.SistemaApiCrud.SistemaCrud.repository.conteudo_clinico_repository;
import com.SistemaApiCrud.SistemaCrud.repository.alternativa_pergunta_repository;
import com.SistemaApiCrud.SistemaCrud.repository.aluno_repository;
import com.SistemaApiCrud.SistemaCrud.repository.caso_clinico_repository;
import com.SistemaApiCrud.SistemaCrud.repository.paciente_repository;
import com.SistemaApiCrud.SistemaCrud.repository.pergunta_repository;
import com.SistemaApiCrud.SistemaCrud.repository.professor_repository;
import com.SistemaApiCrud.SistemaCrud.repository.usuario_repository;
import com.SistemaApiCrud.SistemaCrud.repository.tentativa_caso_repository;
import com.SistemaApiCrud.SistemaCrud.service.caso_clinico_service;
import com.SistemaApiCrud.SistemaCrud.service.CasoClinicoFingerprint;
import com.SistemaApiCrud.SistemaCrud.service.CasoClinicoIaTransactionService;
import com.SistemaApiCrud.SistemaCrud.service.JwtService;
import com.SistemaApiCrud.SistemaCrud.service.conteudo_clinico_service;
import com.SistemaApiCrud.SistemaCrud.service.paciente_service;
import com.SistemaApiCrud.SistemaCrud.service.pergunta_service;
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
    private pergunta_service perguntaService;

    @Autowired
    private CasoClinicoIaTransactionService casoClinicoIaTransactionService;

    @Autowired
    private paciente_service pacienteService;

    @Autowired
    private conteudo_clinico_service conteudoService;

    @Autowired
    private aluno_repository alunoRepository;

    @Autowired
    private professor_repository professorRepository;

    @Autowired
    private caso_clinico_repository casoRepository;

    @Autowired
    private paciente_repository pacienteRepository;

    @Autowired
    private conteudo_clinico_repository conteudoRepository;

    @Autowired
    private pergunta_repository perguntaRepository;

    @Autowired
    private alternativa_pergunta_repository alternativaRepository;

    @Autowired
    private usuario_repository usuarioRepository;

    @Autowired
    private tentativa_caso_repository tentativaRepository;

    @BeforeEach
    void autenticarAdministradorNosTestesDeServico() {
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

        casos_clinicos outroCaso = criarCaso(StatusCasoClinico.PUBLICADO);
        pergunta perguntaDeOutroCaso = criarPergunta(outroCaso, "A");
        responder_caso_request_DTO comPerguntaDeOutroCaso = new responder_caso_request_DTO(List.of(
                new resposta_pergunta_request_DTO(primeira.getId(), "A"),
                new resposta_pergunta_request_DTO(perguntaDeOutroCaso.getId(), "A")));

        assertThatThrownBy(() -> respostaService.responderCaso(
                aluno.getIdAluno(),
                caso.getIdCaso(),
                comPerguntaDeOutroCaso))
                .isInstanceOf(com.SistemaApiCrud.SistemaCrud.exception.BadRequestException.class)
                .hasMessage("Todas as perguntas do caso devem ser respondidas exatamente uma vez");

        assertThat(segunda.getId()).isNotNull();
    }

    @Test
    void deveCorrigirRespostaUsandoAlternativasSeparadas() {
        Aluno aluno = alunoRepository.save(new Aluno(null, "Clara", "clara@email.com", "Medicina", "6"));
        casos_clinicos caso = criarCaso(StatusCasoClinico.PUBLICADO);
        pergunta pergunta = criarPerguntaSemAlternativas(caso, "A");

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
    void deveCorrigirCadaTipoDePerguntaEExcluirPendenciasDaNota() {
        Aluno aluno = alunoRepository.save(new Aluno(
                null,
                "Helena",
                "helena-correcao@email.com",
                "Medicina",
                "6"));
        casos_clinicos caso = criarCaso(StatusCasoClinico.PUBLICADO);
        pergunta verdadeiro = criarPerguntaPorTipo(
                caso,
                TipoPergunta.VERDADEIRO_FALSO,
                "VERDADEIRO");
        pergunta falso = criarPerguntaPorTipo(
                caso,
                TipoPergunta.VERDADEIRO_FALSO,
                "FALSO");
        pergunta abreviacaoInvalida = criarPerguntaPorTipo(
                caso,
                TipoPergunta.VERDADEIRO_FALSO,
                "VERDADEIRO");
        pergunta diagnostico = criarPerguntaPorTipo(
                caso,
                TipoPergunta.DIAGNOSTICO,
                "Pneumonia adquirida na comunidade (PAC)|Pneumônia, comunitária");
        pergunta discursiva = criarPerguntaPorTipo(
                caso,
                TipoPergunta.DISCURSIVA,
                "REVISAO_MANUAL");
        pergunta conduta = criarPerguntaPorTipo(
                caso,
                TipoPergunta.CONDUTA_CLINICA,
                "REVISAO_MANUAL");

        responder_caso_request_DTO requisicao = new responder_caso_request_DTO(List.of(
                new resposta_pergunta_request_DTO(verdadeiro.getId(), "  VÉRDADEIRO  "),
                new resposta_pergunta_request_DTO(falso.getId(), " falso "),
                new resposta_pergunta_request_DTO(abreviacaoInvalida.getId(), "V"),
                new resposta_pergunta_request_DTO(diagnostico.getId(), "pneumonia comunitaria!"),
                new resposta_pergunta_request_DTO(discursiva.getId(), "Resposta argumentada do aluno"),
                new resposta_pergunta_request_DTO(conduta.getId(), "Plano terapeutico do aluno")));

        iniciarTentativa(aluno, caso);
        resultado_caso_DTO resultado = respostaService.responderCaso(
                aluno.getIdAluno(),
                caso.getIdCaso(),
                requisicao);

        assertThat(resultado.getTotalRespondidas()).isEqualTo(6);
        assertThat(resultado.getTotalAvaliadas()).isEqualTo(4);
        assertThat(resultado.getTotalPendentesRevisao()).isEqualTo(2);
        assertThat(resultado.getTotalCorretas()).isEqualTo(3);
        assertThat(resultado.getNota()).isEqualTo(75.0);
        assertThat(resultado.getRespostas())
                .extracting(resposta -> resposta.getCorreta())
                .containsExactly(true, true, false, true, null, null);

        var desempenho = respostaService.buscarDesempenho(aluno.getIdAluno());
        assertThat(desempenho.getTotalRespostas()).isEqualTo(6);
        assertThat(desempenho.getTotalAvaliadas()).isEqualTo(4);
        assertThat(desempenho.getTotalPendentesRevisao()).isEqualTo(2);
        assertThat(desempenho.getTotalCorretas()).isEqualTo(3);
        assertThat(desempenho.getAproveitamento()).isEqualTo(75.0);

        var relatorio = respostaService.gerarRelatorioProfessor(caso.getProfessor().getId());
        assertThat(relatorio.getTotalRespostas()).isEqualTo(6);
        assertThat(relatorio.getTotalAvaliadas()).isEqualTo(4);
        assertThat(relatorio.getTotalPendentesRevisao()).isEqualTo(2);
        assertThat(relatorio.getTotalCorretas()).isEqualTo(3);
        assertThat(relatorio.getAproveitamentoMedio()).isEqualTo(75.0);
    }

    @Test
    void deveDistinguirMarcadoresDiagnosticosPositivoENegativo() {
        Aluno aluno = alunoRepository.save(new Aluno(
                null,
                "Livia",
                "livia-marcador@email.com",
                "Medicina",
                "8"));
        casos_clinicos caso = criarCaso(StatusCasoClinico.PUBLICADO);
        pergunta marcadorPositivo = criarPerguntaPorTipo(
                caso,
                TipoPergunta.DIAGNOSTICO,
                "HER2+");
        pergunta marcadorNegativo = criarPerguntaPorTipo(
                caso,
                TipoPergunta.DIAGNOSTICO,
                "HER2-");
        responder_caso_request_DTO requisicao = new responder_caso_request_DTO(List.of(
                new resposta_pergunta_request_DTO(
                        marcadorPositivo.getId(),
                        "HER2 positivo"),
                new resposta_pergunta_request_DTO(
                        marcadorNegativo.getId(),
                        "HER2 positivo")));

        iniciarTentativa(aluno, caso);
        resultado_caso_DTO resultado = respostaService.responderCaso(
                aluno.getIdAluno(),
                caso.getIdCaso(),
                requisicao);

        assertThat(resultado.getRespostas())
                .extracting(resposta -> resposta.getCorreta())
                .containsExactly(true, false);
        assertThat(resultado.getNota()).isEqualTo(50.0);
    }

    @Test
    void deveRetornarNotaZeroQuandoTodasAsRespostasAguardamRevisao() {
        Aluno aluno = alunoRepository.save(new Aluno(
                null,
                "Igor",
                "igor-revisao@email.com",
                "Medicina",
                "7"));
        casos_clinicos caso = criarCaso(StatusCasoClinico.PUBLICADO);
        pergunta discursiva = criarPerguntaPorTipo(
                caso,
                TipoPergunta.DISCURSIVA,
                "REVISAO_MANUAL");
        responder_caso_request_DTO requisicao = new responder_caso_request_DTO(List.of(
                new resposta_pergunta_request_DTO(
                        discursiva.getId(),
                        "Resposta que sera revisada pelo professor")));

        iniciarTentativa(aluno, caso);
        resultado_caso_DTO resultado = respostaService.responderCaso(
                aluno.getIdAluno(),
                caso.getIdCaso(),
                requisicao);

        assertThat(resultado.getTotalAvaliadas()).isZero();
        assertThat(resultado.getTotalPendentesRevisao()).isEqualTo(1);
        assertThat(resultado.getTotalCorretas()).isZero();
        assertThat(resultado.getNota()).isZero();
        assertThat(resultado.getRespostas().get(0).getCorreta()).isNull();
    }

    @Test
    void deveListarEConcluirRevisaoHumana() {
        Aluno aluno = alunoRepository.save(new Aluno(
                null,
                "Julia",
                "julia-revisao@email.com",
                "Medicina",
                "8"));
        casos_clinicos caso = criarCaso(StatusCasoClinico.PUBLICADO);
        pergunta discursiva = criarPerguntaPorTipo(
                caso,
                TipoPergunta.DISCURSIVA,
                "REVISAO_MANUAL");
        responder_caso_request_DTO requisicao = new responder_caso_request_DTO(List.of(
                new resposta_pergunta_request_DTO(
                        discursiva.getId(),
                        "Resposta fundamentada para revisao")));

        iniciarTentativa(aluno, caso);
        resultado_caso_DTO resultado = respostaService.responderCaso(
                aluno.getIdAluno(),
                caso.getIdCaso(),
                requisicao);
        Long idResposta = resultado.getRespostas().get(0).getId();
        Long idRevisor = usuarioRepository.findByUsername("admin").orElseThrow().getId();

        assertThat(respostaService.listarPendentesRevisao(
                caso.getIdCaso(),
                PageRequest.of(0, 10)).getContent())
                .extracting(resposta -> resposta.getId())
                .containsExactly(idResposta);

        var respostaRevisada = respostaService.revisarResposta(
                caso.getIdCaso(),
                idResposta,
                true,
                "A resposta contempla os criterios essenciais da rubrica.",
                idRevisor);

        assertThat(respostaRevisada.correta()).isTrue();
        assertThat(respostaRevisada.versaoRevisao()).isEqualTo(1L);
        assertThat(respostaService.listarPendentesRevisao(
                caso.getIdCaso(),
                PageRequest.of(0, 10))).isEmpty();
        var desempenho = respostaService.buscarDesempenho(aluno.getIdAluno());
        assertThat(desempenho.getTotalAvaliadas()).isEqualTo(1);
        assertThat(desempenho.getTotalPendentesRevisao()).isZero();
        assertThat(desempenho.getAproveitamento()).isEqualTo(100.0);

        assertThat(respostaService.revisarResposta(
                caso.getIdCaso(),
                idResposta,
                true,
                "A resposta contempla os criterios essenciais da rubrica.",
                idRevisor).correta()).isTrue();

        var revisaoCorrigida = respostaService.revisarResposta(
                caso.getIdCaso(),
                idResposta,
                false,
                "Correcao: faltou justificar a prioridade da conduta.",
                idRevisor);
        assertThat(revisaoCorrigida.correta()).isFalse();
        assertThat(revisaoCorrigida.versaoRevisao()).isEqualTo(2L);
        assertThat(respostaService.listarHistoricoRevisoes(
                caso.getIdCaso(),
                idResposta))
                .extracting(revisao -> revisao.correta())
                .containsExactly(true, false);
        assertThat(respostaService.listarHistoricoRevisoesPaginado(
                caso.getIdCaso(),
                idResposta,
                PageRequest.of(0, 1)).getContent())
                .extracting(revisao -> revisao.correta())
                .containsExactly(true);
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
    void naoDevePublicarCasoSemPacienteConteudoEPergunta() {
        casos_clinicos caso = criarCaso(StatusCasoClinico.RASCUNHO);

        assertThatThrownBy(() -> casoService.publicar(caso.getIdCaso()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cadastre ao menos um paciente antes de publicar o caso clinico");

        pacienteRepository.save(new paciente(
                null,
                caso,
                "Paciente Teste",
                "Professor",
                Sexo.NAO_INFORMADO,
                30,
                EstadoCivil.NAO_INFORMADO,
                "1,70 m",
                "70 kg"));

        assertThatThrownBy(() -> casoService.publicar(caso.getIdCaso()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cadastre ou gere o conteudo clinico antes de publicar o caso clinico");

        conteudo_clinico conteudo = new conteudo_clinico();
        conteudo.setCasoClinico(caso);
        conteudo.setSintomas("Sintomas");
        conteudo.setContexto("Contexto");
        conteudo.setExamClinico("Exame clinico");
        conteudo.setAntecClinico("Antecedentes");
        conteudo.setDiagEsperado("Diagnostico");
        conteudoRepository.save(conteudo);

        assertThatThrownBy(() -> casoService.publicar(caso.getIdCaso()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Cadastre ao menos uma pergunta antes de publicar o caso clinico");

        criarPergunta(caso, "A");
        caso_clinico_response_DTO publicado = casoService.publicar(caso.getIdCaso());

        assertThat(publicado.getStatus()).isEqualTo(StatusCasoClinico.PUBLICADO);
    }

    @Test
    void deveValidarAlternativasDeMultiplaEscolha() {
        casos_clinicos caso = criarCaso(StatusCasoClinico.RASCUNHO);
        pergunta_request_DTO comLetraDuplicada = perguntaMultiplaEscolha(List.of(
                new alternativa_pergunta_DTO(null, "A", "Primeira", true),
                new alternativa_pergunta_DTO(null, "A", "Duplicada", false)));

        assertThatThrownBy(() -> perguntaService.salvarEmCaso(caso.getIdCaso(), comLetraDuplicada))
                .isInstanceOf(com.SistemaApiCrud.SistemaCrud.exception.BadRequestException.class)
                .hasMessage("As letras das alternativas nao podem se repetir");

        pergunta_request_DTO comTextoDuplicado = perguntaMultiplaEscolha(List.of(
                new alternativa_pergunta_DTO(null, "A", "Conduta inicial", true),
                new alternativa_pergunta_DTO(null, "B", "  CONDUTA   INICIAL  ", false)));

        assertThatThrownBy(() -> perguntaService.salvarEmCaso(caso.getIdCaso(), comTextoDuplicado))
                .isInstanceOf(com.SistemaApiCrud.SistemaCrud.exception.BadRequestException.class)
                .hasMessage("Os textos das alternativas nao podem se repetir");

        pergunta_request_DTO semCorreta = perguntaMultiplaEscolha(List.of(
                new alternativa_pergunta_DTO(null, "A", "Primeira", false),
                new alternativa_pergunta_DTO(null, "B", "Segunda", false)));

        assertThatThrownBy(() -> perguntaService.salvarEmCaso(caso.getIdCaso(), semCorreta))
                .isInstanceOf(com.SistemaApiCrud.SistemaCrud.exception.BadRequestException.class)
                .hasMessage("Perguntas de multipla escolha precisam ter exatamente uma alternativa correta");
    }

    @Test
    void deveSalvarLoteDePerguntasEAlternativasAtomicamente() {
        casos_clinicos caso = criarCaso(StatusCasoClinico.RASCUNHO);
        pergunta_request_DTO primeira = perguntaMultiplaEscolha(List.of(
                new alternativa_pergunta_DTO(null, "A", "Conduta correta 1", true),
                new alternativa_pergunta_DTO(null, "B", "Distrator 1", false)));
        pergunta_request_DTO segunda = perguntaMultiplaEscolha(List.of(
                new alternativa_pergunta_DTO(null, "A", "Conduta correta 2", true),
                new alternativa_pergunta_DTO(null, "B", "Distrator 2", false)));
        primeira.setTexto("Primeira pergunta gerada");
        segunda.setTexto("Segunda pergunta gerada");

        var respostas = perguntaService.salvarLoteEmCaso(
                caso.getIdCaso(),
                List.of(primeira, segunda));

        assertThat(respostas).hasSize(2);
        assertThat(perguntaRepository.findByCasoClinicoIdCaso(caso.getIdCaso())).hasSize(2);
        assertThat(respostas)
                .allSatisfy(resposta -> assertThat(resposta.getAlternativas()).hasSize(2));
    }

    @Test
    void loteInvalidoNaoDevePersistirNenhumaPergunta() {
        casos_clinicos caso = criarCaso(StatusCasoClinico.RASCUNHO);
        pergunta_request_DTO valida = perguntaMultiplaEscolha(List.of(
                new alternativa_pergunta_DTO(null, "A", "Correta", true),
                new alternativa_pergunta_DTO(null, "B", "Incorreta", false)));
        pergunta_request_DTO invalida = perguntaMultiplaEscolha(List.of(
                new alternativa_pergunta_DTO(null, "A", "Primeira", false),
                new alternativa_pergunta_DTO(null, "B", "Segunda", false)));

        assertThatThrownBy(() -> perguntaService.salvarLoteEmCaso(
                caso.getIdCaso(),
                List.of(valida, invalida)))
                .isInstanceOf(com.SistemaApiCrud.SistemaCrud.exception.BadRequestException.class);

        assertThat(perguntaRepository.findByCasoClinicoIdCaso(caso.getIdCaso())).isEmpty();
    }

    @Test
    void loteDeveRevalidarStatusDoCasoAntesDePersistir() {
        casos_clinicos caso = criarCaso(StatusCasoClinico.PUBLICADO);
        pergunta_request_DTO pergunta = perguntaMultiplaEscolha(List.of(
                new alternativa_pergunta_DTO(null, "A", "Correta", true),
                new alternativa_pergunta_DTO(null, "B", "Incorreta", false)));

        assertThatThrownBy(() -> perguntaService.salvarLoteEmCaso(
                caso.getIdCaso(),
                List.of(pergunta)))
                .isInstanceOf(com.SistemaApiCrud.SistemaCrud.exception.ConflitoEstadoException.class)
                .hasMessageContaining("rascunho");

        assertThat(perguntaRepository.findByCasoClinicoIdCaso(caso.getIdCaso())).isEmpty();
    }

    @Test
    void loteNaoDevePersistirSeContextoMudouDuranteGeracao() {
        casos_clinicos caso = criarCaso(StatusCasoClinico.RASCUNHO);
        conteudo_clinico conteudo = new conteudo_clinico();
        conteudo.setCasoClinico(caso);
        conteudo.setSintomas("Tosse");
        conteudo.setContexto("Atendimento inicial");
        conteudo.setExamClinico("Crepitacoes");
        conteudo.setAntecClinico("Sem antecedentes");
        conteudo.setDiagEsperado("Pneumonia");
        conteudo = conteudoRepository.saveAndFlush(conteudo);
        String fingerprintOriginal = CasoClinicoFingerprint.calcular(caso, conteudo, List.of());

        conteudo.setDiagEsperado("Diagnostico alterado");
        conteudoRepository.saveAndFlush(conteudo);
        pergunta_request_DTO pergunta = perguntaMultiplaEscolha(List.of(
                new alternativa_pergunta_DTO(null, "A", "Correta", true),
                new alternativa_pergunta_DTO(null, "B", "Incorreta", false)));

        assertThatThrownBy(() -> perguntaService.salvarLoteEmCaso(
                caso.getIdCaso(),
                List.of(pergunta),
                fingerprintOriginal))
                .isInstanceOf(com.SistemaApiCrud.SistemaCrud.exception.ConflitoEstadoException.class)
                .hasMessageContaining("mudou durante a geracao");

        assertThat(perguntaRepository.findByCasoClinicoIdCaso(caso.getIdCaso())).isEmpty();
    }

    @Test
    void loteNaoDevePersistirSePerguntasMudaramDuranteGeracao() {
        casos_clinicos caso = criarCaso(StatusCasoClinico.RASCUNHO);
        conteudo_clinico conteudo = new conteudo_clinico();
        conteudo.setCasoClinico(caso);
        conteudo.setSintomas("Tosse");
        conteudo.setContexto("Atendimento inicial");
        conteudo.setExamClinico("Crepitacoes");
        conteudo.setAntecClinico("Sem antecedentes");
        conteudo.setDiagEsperado("Pneumonia");
        conteudo = conteudoRepository.saveAndFlush(conteudo);
        String fingerprint = CasoClinicoFingerprint.calcular(caso, conteudo, List.of());

        pergunta_request_DTO perguntaExistente = perguntaMultiplaEscolha(List.of(
                new alternativa_pergunta_DTO(null, "A", "Correta", true),
                new alternativa_pergunta_DTO(null, "B", "Incorreta", false)));
        perguntaExistente.setTexto("Pergunta criada durante a chamada ao provedor");
        perguntaService.salvarEmCaso(caso.getIdCaso(), perguntaExistente);

        pergunta_request_DTO perguntaGerada = perguntaMultiplaEscolha(List.of(
                new alternativa_pergunta_DTO(null, "A", "Correta", true),
                new alternativa_pergunta_DTO(null, "B", "Incorreta", false)));
        perguntaGerada.setTexto("Pergunta gerada com contexto anterior");

        assertThatThrownBy(() -> perguntaService.salvarLoteEmCaso(
                caso.getIdCaso(),
                List.of(perguntaGerada),
                fingerprint,
                0L))
                .isInstanceOf(com.SistemaApiCrud.SistemaCrud.exception.ConflitoEstadoException.class)
                .hasMessageContaining("perguntas do caso clinico mudaram");

        assertThat(perguntaRepository.findByCasoClinicoIdCaso(caso.getIdCaso()))
                .singleElement()
                .extracting(pergunta::getTexto)
                .isEqualTo("Pergunta criada durante a chamada ao provedor");
    }

    @Test
    void loteGeradoNaoDeveDuplicarEnunciadoJaExistente() {
        casos_clinicos caso = criarCaso(StatusCasoClinico.RASCUNHO);
        conteudo_clinico conteudo = new conteudo_clinico();
        conteudo.setCasoClinico(caso);
        conteudo.setSintomas("Tosse");
        conteudo.setContexto("Atendimento inicial");
        conteudo.setExamClinico("Crepitacoes");
        conteudo.setAntecClinico("Sem antecedentes");
        conteudo.setDiagEsperado("Pneumonia");
        conteudo = conteudoRepository.saveAndFlush(conteudo);
        String fingerprint = CasoClinicoFingerprint.calcular(caso, conteudo, List.of());

        pergunta_request_DTO perguntaExistente = perguntaMultiplaEscolha(List.of(
                new alternativa_pergunta_DTO(null, "A", "Correta", true),
                new alternativa_pergunta_DTO(null, "B", "Incorreta", false)));
        perguntaExistente.setTexto("Qual e a melhor conduta?");
        perguntaService.salvarEmCaso(caso.getIdCaso(), perguntaExistente);

        pergunta_request_DTO perguntaDuplicada = perguntaMultiplaEscolha(List.of(
                new alternativa_pergunta_DTO(null, "A", "Correta", true),
                new alternativa_pergunta_DTO(null, "B", "Incorreta", false)));
        perguntaDuplicada.setTexto("  QUAL   E A MELHOR CONDUTA? ");

        assertThatThrownBy(() -> perguntaService.salvarLoteEmCaso(
                caso.getIdCaso(),
                List.of(perguntaDuplicada),
                fingerprint,
                1L))
                .isInstanceOf(com.SistemaApiCrud.SistemaCrud.exception.ConflitoEstadoException.class)
                .hasMessageContaining("ja existe neste caso clinico");
        assertThat(perguntaRepository.findByCasoClinicoIdCaso(caso.getIdCaso())).hasSize(1);
    }

    @Test
    void persistenciaDeConteudoIaDeveFazerRollbackAtomico() {
        casos_clinicos caso = criarCaso(StatusCasoClinico.RASCUNHO);
        String tituloOriginal = caso.getTitulo();
        conteudo_clinico conteudoOriginal = new conteudo_clinico();
        conteudoOriginal.setCasoClinico(caso);
        conteudoOriginal.setSintomas("Tosse");
        conteudoOriginal.setContexto("Atendimento inicial");
        conteudoOriginal.setExamClinico("Crepitacoes");
        conteudoOriginal.setAntecClinico("Sem antecedentes");
        conteudoOriginal.setDiagEsperado("Pneumonia");
        conteudoOriginal = conteudoRepository.saveAndFlush(conteudoOriginal);
        String fingerprint = CasoClinicoFingerprint.calcular(
                caso,
                conteudoOriginal,
                List.of());

        assertThatThrownBy(() -> casoClinicoIaTransactionService.executarGeracao(
                caso.getIdCaso(),
                fingerprint,
                List.of(),
                casoBloqueado -> {
                    casoBloqueado.setTitulo("Titulo que deve sofrer rollback");
                    conteudo_clinico novoConteudo = new conteudo_clinico();
                    novoConteudo.setCasoClinico(casoBloqueado);
                    novoConteudo.setSintomas("Novo sintoma");
                    novoConteudo.setContexto("Novo contexto");
                    novoConteudo.setExamClinico("Novo exame");
                    novoConteudo.setAntecClinico("Novo antecedente");
                    novoConteudo.setDiagEsperado("Novo diagnostico");
                    conteudoRepository.save(novoConteudo);
                    throw new BusinessException("Falha simulada durante a persistencia");
                }))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Falha simulada durante a persistencia");

        assertThat(casoRepository.findById(caso.getIdCaso()).orElseThrow().getTitulo())
                .isEqualTo(tituloOriginal);
        assertThat(conteudoRepository.findByCasoClinicoIdCaso(caso.getIdCaso()))
                .singleElement()
                .extracting(conteudo_clinico::getIdConteudo)
                .isEqualTo(conteudoOriginal.getIdConteudo());
    }

    @Test
    void persistenciaDeConteudoIaDeveRejeitarContextoAlterado() {
        casos_clinicos caso = criarCaso(StatusCasoClinico.RASCUNHO);
        conteudo_clinico conteudo = new conteudo_clinico();
        conteudo.setCasoClinico(caso);
        conteudo.setSintomas("Tosse");
        conteudo.setContexto("Atendimento inicial");
        conteudo.setExamClinico("Crepitacoes");
        conteudo.setAntecClinico("Sem antecedentes");
        conteudo.setDiagEsperado("Pneumonia");
        conteudo = conteudoRepository.saveAndFlush(conteudo);
        String fingerprintAnterior = CasoClinicoFingerprint.calcular(
                caso,
                conteudo,
                List.of());

        conteudo.setDiagEsperado("Diagnostico alterado");
        conteudoRepository.saveAndFlush(conteudo);
        AtomicBoolean executouPersistencia = new AtomicBoolean(false);

        assertThatThrownBy(() -> casoClinicoIaTransactionService.executarGeracao(
                caso.getIdCaso(),
                fingerprintAnterior,
                List.of(),
                casoBloqueado -> {
                    executouPersistencia.set(true);
                    return null;
                }))
                .isInstanceOf(com.SistemaApiCrud.SistemaCrud.exception.ConflitoEstadoException.class)
                .hasMessageContaining("mudou durante a operacao com IA");
        assertThat(executouPersistencia).isFalse();
    }

    @Test
    void persistenciaDeConteudoIaDeveDetectarAlteracaoNoSegundoPaciente() {
        casos_clinicos caso = criarCaso(StatusCasoClinico.RASCUNHO);
        paciente primeiroPaciente = pacienteRepository.saveAndFlush(new paciente(
                null,
                caso,
                "Paciente um",
                "Professor",
                Sexo.NAO_INFORMADO,
                40,
                EstadoCivil.NAO_INFORMADO,
                "1,70 m",
                "70 kg"));
        paciente segundoPaciente = pacienteRepository.saveAndFlush(new paciente(
                null,
                caso,
                "Paciente dois",
                "Enfermeiro",
                Sexo.NAO_INFORMADO,
                35,
                EstadoCivil.NAO_INFORMADO,
                "1,65 m",
                "65 kg"));
        List<paciente> pacientes = List.of(primeiroPaciente, segundoPaciente);
        String fingerprintAnterior = CasoClinicoFingerprint.calcular(caso, null, pacientes);

        segundoPaciente.setProfissao("Medico");
        pacienteRepository.saveAndFlush(segundoPaciente);
        AtomicBoolean executouPersistencia = new AtomicBoolean(false);

        assertThatThrownBy(() -> casoClinicoIaTransactionService.executarGeracao(
                caso.getIdCaso(),
                fingerprintAnterior,
                pacientes.stream().map(paciente::getIdPaciente).toList(),
                casoBloqueado -> {
                    executouPersistencia.set(true);
                    return null;
                }))
                .isInstanceOf(com.SistemaApiCrud.SistemaCrud.exception.ConflitoEstadoException.class)
                .hasMessageContaining("mudou durante a operacao com IA");
        assertThat(executouPersistencia).isFalse();
    }

    @Test
    void casoPublicadoNaoDevePermitirAlterarSeusComponentes() {
        casos_clinicos caso = criarCaso(StatusCasoClinico.PUBLICADO);
        paciente paciente = pacienteRepository.saveAndFlush(new paciente(
                null,
                caso,
                "Paciente original",
                "Professor",
                Sexo.NAO_INFORMADO,
                40,
                EstadoCivil.NAO_INFORMADO,
                "1,70 m",
                "70 kg"));
        paciente_DTO pacienteAtualizado = new paciente_DTO(
                paciente.getIdPaciente(),
                caso.getIdCaso(),
                "Paciente alterado",
                "Professor",
                Sexo.NAO_INFORMADO,
                40,
                EstadoCivil.NAO_INFORMADO,
                "1,70 m",
                "70 kg");

        assertThatThrownBy(() -> pacienteService.atualizar(
                paciente.getIdPaciente(),
                pacienteAtualizado))
                .isInstanceOf(com.SistemaApiCrud.SistemaCrud.exception.ConflitoEstadoException.class)
                .hasMessageContaining("rascunho");
        assertThat(pacienteRepository.findById(paciente.getIdPaciente()).orElseThrow().getNome())
                .isEqualTo("Paciente original");

        conteudo_clinico conteudo = new conteudo_clinico();
        conteudo.setCasoClinico(caso);
        conteudo.setSintomas("Sintoma original");
        conteudo.setContexto("Contexto");
        conteudo.setExamClinico("Exame");
        conteudo.setAntecClinico("Antecedente");
        conteudo.setDiagEsperado("Diagnostico");
        conteudo = conteudoRepository.saveAndFlush(conteudo);
        conteudo_clinico_DTO conteudoAtualizado = new conteudo_clinico_DTO(
                conteudo.getIdConteudo(),
                caso.getIdCaso(),
                "Sintoma alterado",
                "Contexto",
                "Exame",
                "Antecedente",
                "Diagnostico");

        Long idConteudo = conteudo.getIdConteudo();
        assertThatThrownBy(() -> conteudoService.atualizar(idConteudo, conteudoAtualizado))
                .isInstanceOf(com.SistemaApiCrud.SistemaCrud.exception.ConflitoEstadoException.class)
                .hasMessageContaining("rascunho");
        assertThat(conteudoRepository.findById(idConteudo).orElseThrow().getSintomas())
                .isEqualTo("Sintoma original");
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
        caso.setNivelDificuldade(NivelDificuldade.MEDIA);
        caso.setDisciplina("Clinica Medica");
        caso.setAreaSaude("Medicina");
        caso.setEstilo("Multipla escolha");
        caso.setEspecialidade("Pneumologia");
        caso.setStatus(status);
        caso.setTempoLimiteMinutos(60);

        return casoRepository.save(caso);
    }

    private pergunta criarPergunta(casos_clinicos caso, String gabarito) {
        pergunta perguntaSalva = criarPerguntaSemAlternativas(caso, gabarito);
        alternativaRepository.saveAll(List.of(
                new AlternativaPergunta(null, perguntaSalva, "A", "A", "A".equals(gabarito)),
                new AlternativaPergunta(null, perguntaSalva, "B", "B", "B".equals(gabarito)),
                new AlternativaPergunta(null, perguntaSalva, "C", "C", "C".equals(gabarito)),
                new AlternativaPergunta(null, perguntaSalva, "D", "D", "D".equals(gabarito)),
                new AlternativaPergunta(null, perguntaSalva, "E", "E", "E".equals(gabarito))));
        return perguntaSalva;
    }

    private pergunta criarPerguntaSemAlternativas(casos_clinicos caso, String gabarito) {
        pergunta pergunta = new pergunta();
        pergunta.setCasoClinico(caso);
        pergunta.setTexto("Qual a melhor conduta?");
        pergunta.setResposta(gabarito);
        pergunta.setTipo(TipoPergunta.MULTIPLA_ESCOLHA);
        pergunta.setGabarito(gabarito);

        return perguntaRepository.save(pergunta);
    }

    private pergunta criarPerguntaPorTipo(
            casos_clinicos caso,
            TipoPergunta tipo,
            String gabarito) {
        pergunta novaPergunta = new pergunta();
        novaPergunta.setCasoClinico(caso);
        novaPergunta.setTexto("Pergunta para " + tipo);
        novaPergunta.setResposta(gabarito);
        novaPergunta.setTipo(tipo);
        novaPergunta.setGabarito(gabarito);

        return perguntaRepository.save(novaPergunta);
    }

    private pergunta_request_DTO perguntaMultiplaEscolha(List<alternativa_pergunta_DTO> alternativas) {
        pergunta_request_DTO dto = new pergunta_request_DTO();
        dto.setTexto("Qual a melhor conduta?");
        dto.setTipo(TipoPergunta.MULTIPLA_ESCOLHA);
        dto.setGabarito("A");
        dto.setResposta("A");
        dto.setAlternativas(alternativas);
        return dto;
    }

    private void iniciarTentativa(Aluno aluno, casos_clinicos caso) {
        casoService.buscarCompletoPublicadoPorId(caso.getIdCaso(), aluno.getIdAluno());
    }
}
