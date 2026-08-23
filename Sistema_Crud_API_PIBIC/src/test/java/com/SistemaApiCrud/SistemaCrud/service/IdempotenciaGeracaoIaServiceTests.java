package com.SistemaApiCrud.SistemaCrud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.SistemaApiCrud.SistemaCrud.entity.SolicitacaoGeracaoIa;
import com.SistemaApiCrud.SistemaCrud.entity.Usuario;
import com.SistemaApiCrud.SistemaCrud.entity.enums.EstadoSolicitacaoGeracaoIa;
import com.SistemaApiCrud.SistemaCrud.entity.enums.OperacaoGeracaoIa;
import com.SistemaApiCrud.SistemaCrud.exception.ConflitoIdempotenciaException;
import com.SistemaApiCrud.SistemaCrud.exception.ApiExceptionHandler;
import com.SistemaApiCrud.SistemaCrud.exception.ApiProblem;
import com.SistemaApiCrud.SistemaCrud.exception.ApiProblemSupport;
import com.SistemaApiCrud.SistemaCrud.exception.SolicitacaoIaEmAndamentoException;
import com.SistemaApiCrud.SistemaCrud.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class IdempotenciaGeracaoIaServiceTests {

    private static final String CHAVE = "f7b0a585-87b7-4995-8f86-809f8395b646";

    private final IdempotenciaGeracaoIaStore store = mock(IdempotenciaGeracaoIaStore.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final IdempotenciaGeracaoIaService service = new IdempotenciaGeracaoIaService(
            store,
            usuarioRepository);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @AfterEach
    void limparAutenticacao() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void devePreservarFluxoAtualSemCabecalho() {
        AtomicInteger chamadas = new AtomicInteger();

        RespostaIdempotente<String> resposta = service.executar(
                null,
                OperacaoGeracaoIa.GERAR_CASO,
                7L,
                java.util.Map.of("campo", "valor"),
                200,
                solicitacao -> "nao-usado",
                () -> "resultado-ia-" + chamadas.incrementAndGet());

        assertThat(resposta.corpo()).isEqualTo("resultado-ia-1");
        verify(store, never()).iniciar(any(), any(), any(), any(), any());
    }

    @Test
    void deveReproduzirRespostaConcluidaSemExecutarProvedor() throws Exception {
        Usuario usuario = autenticar();
        SolicitacaoGeracaoIa solicitacao = solicitacao(usuario, EstadoSolicitacaoGeracaoIa.CONCLUIDA);
        solicitacao.setStatusResposta(201);
        solicitacao.setIdsResultado("42");
        when(store.iniciar(any(), any(), any(), any(), any()))
                .thenReturn(new InicioIdempotenciaIa(solicitacao, false));

        RespostaIdempotente<String> resposta = service.executar(
                CHAVE,
                OperacaoGeracaoIa.GERAR_CASO,
                7L,
                java.util.Map.of("campo", "valor"),
                201,
                item -> "resultado-cache",
                () -> {
                    throw new AssertionError("O provedor nao deve ser chamado no replay");
                });

        assertThat(resposta.status()).isEqualTo(201);
        assertThat(resposta.corpo()).isEqualTo("resultado-cache");
    }

    @Test
    void deveReproduzirListaDePerguntasConcluida() {
        Usuario usuario = autenticar();
        SolicitacaoGeracaoIa solicitacao = solicitacao(usuario, EstadoSolicitacaoGeracaoIa.CONCLUIDA);
        solicitacao.setOperacao(OperacaoGeracaoIa.GERAR_PERGUNTAS);
        solicitacao.setHashRequisicao(hashPara(OperacaoGeracaoIa.GERAR_PERGUNTAS));
        solicitacao.setStatusResposta(201);
        solicitacao.setIdsResultado("3");
        when(store.iniciar(any(), any(), any(), any(), any()))
                .thenReturn(new InicioIdempotenciaIa(solicitacao, false));

        RespostaIdempotente<java.util.List<java.util.Map<String, Object>>> resposta = service.executar(
                CHAVE,
                OperacaoGeracaoIa.GERAR_PERGUNTAS,
                7L,
                java.util.Map.of("campo", "valor"),
                201,
                item -> java.util.List.of(java.util.Map.of("id", 3, "texto", "Pergunta")),
                () -> {
                    throw new AssertionError("O provedor nao deve ser chamado no replay");
                });

        assertThat(resposta.status()).isEqualTo(201);
        assertThat(resposta.corpo()).hasSize(1);
        assertThat(resposta.corpo().get(0).get("texto")).isEqualTo("Pergunta");
    }

    @Test
    void deveConflitarQuandoMesmaChaveRepresentaOutroPayload() {
        Usuario usuario = autenticar();
        SolicitacaoGeracaoIa solicitacao = solicitacao(usuario, EstadoSolicitacaoGeracaoIa.CONCLUIDA);
        solicitacao.setHashRequisicao("0".repeat(64));
        when(store.iniciar(any(), any(), any(), any(), any()))
                .thenReturn(new InicioIdempotenciaIa(solicitacao, false));

        assertThatThrownBy(() -> service.executar(
                CHAVE,
                OperacaoGeracaoIa.GERAR_CASO,
                7L,
                java.util.Map.of("campo", "valor"),
                200,
                item -> "nao-usado",
                () -> "nao-deve-executar"))
                .isInstanceOf(ConflitoIdempotenciaException.class);
    }

    @Test
    void deveRetornarConflitoEnquantoSolicitacaoEstaEmAndamento() {
        Usuario usuario = autenticar();
        SolicitacaoGeracaoIa solicitacao = solicitacao(usuario, EstadoSolicitacaoGeracaoIa.EM_ANDAMENTO);
        when(store.iniciar(any(), any(), any(), any(), any()))
                .thenReturn(new InicioIdempotenciaIa(solicitacao, false));

        assertThatThrownBy(() -> service.executar(
                CHAVE,
                OperacaoGeracaoIa.GERAR_CASO,
                7L,
                java.util.Map.of("campo", "valor"),
                200,
                item -> "nao-usado",
                () -> "nao-deve-executar"))
                .isInstanceOf(SolicitacaoIaEmAndamentoException.class);
    }

    @Test
    void deveExporRetryAfterNoConflitoEmAndamento() {
        ApiExceptionHandler handler = new ApiExceptionHandler(new ApiProblemSupport());
        MockHttpServletRequest requisicao = new MockHttpServletRequest("POST", "/casos/7/ia/perguntas/gerar");

        ResponseEntity<ApiProblem> resposta = handler.tratarSolicitacaoIaEmAndamento(
                new SolicitacaoIaEmAndamentoException(2),
                requisicao);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("2");
        assertThat(resposta.getBody().type()).endsWith("solicitacao-ia-em-andamento");
    }

    @Test
    void deveMarcarFalhaEExigirNovaChaveAposErroDoProvedor() {
        Usuario usuario = autenticar();
        SolicitacaoGeracaoIa solicitacao = solicitacao(usuario, EstadoSolicitacaoGeracaoIa.EM_ANDAMENTO);
        when(store.iniciar(any(), any(), any(), any(), any()))
                .thenReturn(new InicioIdempotenciaIa(solicitacao, true));

        assertThatThrownBy(() -> service.executar(
                CHAVE,
                OperacaoGeracaoIa.GERAR_CASO,
                7L,
                java.util.Map.of("campo", "valor"),
                200,
                item -> "nao-usado",
                () -> {
                    throw new IllegalStateException("falha do provedor");
                }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("falha do provedor");
        verify(store).falhar(91L);
    }

    @Test
    void deveManterSucessoQuandoEnriquecimentoFalhaAposConclusaoAtomica() {
        Usuario usuario = autenticar();
        SolicitacaoGeracaoIa emAndamento = solicitacao(usuario, EstadoSolicitacaoGeracaoIa.EM_ANDAMENTO);
        SolicitacaoGeracaoIa concluida = solicitacao(usuario, EstadoSolicitacaoGeracaoIa.CONCLUIDA);
        concluida.setStatusResposta(200);
        concluida.setIdsResultado("42");
        when(store.iniciar(any(), any(), any(), any(), any()))
                .thenReturn(new InicioIdempotenciaIa(emAndamento, true));
        when(store.buscar(12L, CHAVE)).thenReturn(concluida);

        RespostaIdempotente<String> resposta = service.executar(
                CHAVE,
                OperacaoGeracaoIa.GERAR_CASO,
                7L,
                java.util.Map.of("campo", "valor"),
                200,
                item -> "resposta-reconstruida",
                () -> {
                    throw new IllegalStateException("falha ao enriquecer");
                });

        assertThat(resposta.status()).isEqualTo(200);
        assertThat(resposta.corpo()).isEqualTo("resposta-reconstruida");
        verify(store, never()).falhar(91L);
    }

    private Usuario autenticar() {
        Usuario usuario = new Usuario();
        usuario.setId(12L);
        usuario.setUsername("professor-idempotente");
        when(usuarioRepository.findByUsername(usuario.getUsername())).thenReturn(Optional.of(usuario));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        usuario.getUsername(),
                        null,
                        java.util.List.of()));
        return usuario;
    }

    private SolicitacaoGeracaoIa solicitacao(Usuario usuario, EstadoSolicitacaoGeracaoIa estado) {
        SolicitacaoGeracaoIa solicitacao = new SolicitacaoGeracaoIa();
        solicitacao.setId(91L);
        solicitacao.setUsuario(usuario);
        solicitacao.setIdCaso(7L);
        solicitacao.setOperacao(OperacaoGeracaoIa.GERAR_CASO);
        solicitacao.setChaveIdempotencia(CHAVE);
        solicitacao.setHashRequisicao(hashPara(OperacaoGeracaoIa.GERAR_CASO));
        solicitacao.setEstado(estado);
        solicitacao.setCriadoEm(Instant.now());
        solicitacao.setAtualizadoEm(Instant.now());
        solicitacao.setExpiraEm(Instant.now().plusSeconds(3600));
        return solicitacao;
    }

    private String hashPara(OperacaoGeracaoIa operacao) {
        try {
            String requisicao = objectMapper.writeValueAsString(java.util.Map.of("campo", "valor"));
            java.security.MessageDigest resumo = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(
                    resumo.digest((operacao.name() + "|7|" + requisicao)
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
