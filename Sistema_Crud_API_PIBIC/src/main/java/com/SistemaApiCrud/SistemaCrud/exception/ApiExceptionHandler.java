package com.SistemaApiCrud.SistemaCrud.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final ApiProblemSupport problemas;

    public ApiExceptionHandler(ApiProblemSupport problemas) {
        this.problemas = problemas;
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ApiProblem> tratarRecursoNaoEncontrado(
            RecursoNaoEncontradoException ex,
            HttpServletRequest requisicao) {
        return problemas.responder(
                HttpStatus.NOT_FOUND,
                "recurso-nao-encontrado",
                "Recurso nao encontrado",
                ex.getMessage(),
                requisicao);
    }

    @ExceptionHandler({BadRequestException.class, BusinessException.class})
    public ResponseEntity<ApiProblem> tratarRequisicaoInvalida(
            RuntimeException ex,
            HttpServletRequest requisicao) {
        return problemas.responder(
                HttpStatus.BAD_REQUEST,
                "requisicao-invalida",
                "Requisicao invalida",
                ex.getMessage(),
                requisicao);
    }

    @ExceptionHandler(ConflitoEstadoException.class)
    public ResponseEntity<ApiProblem> tratarConflitoDeEstado(
            ConflitoEstadoException ex,
            HttpServletRequest requisicao) {
        return problemas.responder(
                HttpStatus.CONFLICT,
                "conflito-de-estado",
                "Conflito de estado",
                ex.getMessage(),
                requisicao);
    }

    @ExceptionHandler(ConflitoIdempotenciaException.class)
    public ResponseEntity<ApiProblem> tratarConflitoIdempotencia(
            ConflitoIdempotenciaException ex,
            HttpServletRequest requisicao) {
        return problemas.responder(
                HttpStatus.CONFLICT,
                "conflito-idempotencia",
                "Conflito de idempotencia",
                ex.getMessage(),
                requisicao);
    }

    @ExceptionHandler(SolicitacaoIaEmAndamentoException.class)
    public ResponseEntity<ApiProblem> tratarSolicitacaoIaEmAndamento(
            SolicitacaoIaEmAndamentoException ex,
            HttpServletRequest requisicao) {
        return comRetryAfter(
                HttpStatus.CONFLICT,
                "solicitacao-ia-em-andamento",
                "Solicitacao de IA em andamento",
                ex.getMessage(),
                ex.getRetryAfterSeconds(),
                requisicao);
    }

    @ExceptionHandler(AiProviderException.class)
    public ResponseEntity<ApiProblem> tratarFalhaDoProvedorIa(
            AiProviderException ex,
            HttpServletRequest requisicao) {
        return problemas.responder(
                HttpStatus.BAD_GATEWAY,
                "falha-provedor-ia",
                "Falha no provedor de IA",
                ex.getMessage(),
                requisicao);
    }

    @ExceptionHandler(ServicoIndisponivelException.class)
    public ResponseEntity<ApiProblem> tratarServicoIndisponivel(
            ServicoIndisponivelException ex,
            HttpServletRequest requisicao) {
        return problemas.responder(
                HttpStatus.SERVICE_UNAVAILABLE,
                "servico-indisponivel",
                "Servico indisponivel",
                ex.getMessage(),
                requisicao);
    }

    @ExceptionHandler(CapacidadeIaEsgotadaException.class)
    public ResponseEntity<ApiProblem> tratarCapacidadeIaEsgotada(
            CapacidadeIaEsgotadaException ex,
            HttpServletRequest requisicao) {
        return comRetryAfter(
                HttpStatus.SERVICE_UNAVAILABLE,
                "capacidade-ia-esgotada",
                "Capacidade de IA esgotada",
                ex.getMessage(),
                ex.getSegundosAteNovaTentativa(),
                requisicao);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiProblem> tratarAutenticacao(HttpServletRequest requisicao) {
        return problemas.responder(
                HttpStatus.UNAUTHORIZED,
                "nao-autenticado",
                "Autenticacao necessaria",
                "Credenciais invalidas",
                requisicao);
    }

    @ExceptionHandler(MuitasTentativasLoginException.class)
    public ResponseEntity<ApiProblem> tratarMuitasTentativas(
            MuitasTentativasLoginException ex,
            HttpServletRequest requisicao) {
        return comRetryAfter(
                HttpStatus.TOO_MANY_REQUESTS,
                "muitas-tentativas-login",
                "Muitas tentativas",
                ex.getMessage(),
                ex.getRetryAfterSeconds(),
                requisicao);
    }

    @ExceptionHandler(LimiteUsoIaException.class)
    public ResponseEntity<ApiProblem> tratarLimiteUsoIa(
            LimiteUsoIaException ex,
            HttpServletRequest requisicao) {
        return comRetryAfter(
                HttpStatus.TOO_MANY_REQUESTS,
                "limite-uso-ia",
                "Limite de uso da IA",
                ex.getMessage(),
                ex.getSegundosAteNovaTentativa(),
                requisicao);
    }

    @ExceptionHandler(TempoEsgotadoIaException.class)
    public ResponseEntity<ApiProblem> tratarTempoEsgotadoIa(
            TempoEsgotadoIaException ex,
            HttpServletRequest requisicao) {
        return problemas.responder(
                HttpStatus.GATEWAY_TIMEOUT,
                "tempo-esgotado-ia",
                "Tempo esgotado no provedor de IA",
                ex.getMessage(),
                requisicao);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiProblem> tratarAcessoNegado(HttpServletRequest requisicao) {
        return problemas.responder(
                HttpStatus.FORBIDDEN,
                "acesso-negado",
                "Acesso negado",
                "O usuario nao possui permissao para esta operacao",
                requisicao);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiProblem> tratarValidacao(
            MethodArgumentNotValidException ex,
            HttpServletRequest requisicao) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> campos.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> campos.putIfAbsent("request", error.getDefaultMessage()));
        return problemaDeValidacao(campos, requisicao);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiProblem> tratarViolacaoDeRestricao(
            ConstraintViolationException ex,
            HttpServletRequest requisicao) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(error -> campos.putIfAbsent(
                error.getPropertyPath().toString(),
                error.getMessage()));
        return problemaDeValidacao(campos, requisicao);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiProblem> tratarEntradaMalformada(
            Exception ex,
            HttpServletRequest requisicao) {
        return problemas.responder(
                HttpStatus.BAD_REQUEST,
                "entrada-malformada",
                "Entrada malformada",
                "A requisicao possui JSON, parametros ou tipos invalidos",
                requisicao);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiProblem> tratarMetodoNaoPermitido(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest requisicao) {
        return problemas.responder(
                HttpStatus.METHOD_NOT_ALLOWED,
                "metodo-nao-permitido",
                "Metodo nao permitido",
                "O metodo HTTP nao e aceito para este recurso",
                requisicao);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiProblem> tratarMidiaNaoSuportada(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest requisicao) {
        return problemas.responder(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "midia-nao-suportada",
                "Tipo de midia nao suportado",
                "Use um Content-Type aceito por este recurso",
                requisicao);
    }

    @ExceptionHandler({DataIntegrityViolationException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<ApiProblem> tratarConflitoDePersistencia(
            RuntimeException ex,
            HttpServletRequest requisicao) {
        return problemas.responder(
                HttpStatus.CONFLICT,
                "conflito-de-persistencia",
                "Conflito de persistencia",
                "Nao foi possivel concluir a operacao porque os dados foram alterados ou possuem relacionamentos ativos",
                requisicao);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiProblem> tratarErroInesperado(
            Exception ex,
            HttpServletRequest requisicao) {
        ApiProblem problema = problemas.criar(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "erro-interno",
                "Erro interno",
                "Nao foi possivel concluir a operacao",
                requisicao);
        LOGGER.error(
                "Erro interno na requisicao {} (correlationId={})",
                requisicao.getRequestURI(),
                problema.correlationId(),
                ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
                .body(problema);
    }

    private ResponseEntity<ApiProblem> problemaDeValidacao(
            Map<String, String> campos,
            HttpServletRequest requisicao) {
        return problemas.responder(
                HttpStatus.BAD_REQUEST,
                "dados-invalidos",
                "Dados invalidos",
                "Um ou mais campos da requisicao sao invalidos",
                requisicao,
                campos);
    }

    private ResponseEntity<ApiProblem> comRetryAfter(
            HttpStatus status,
            String codigo,
            String titulo,
            String detalhe,
            long segundos,
            HttpServletRequest requisicao) {
        return ResponseEntity.status(status)
                .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(Math.max(1, segundos)))
                .body(problemas.criar(status, codigo, titulo, detalhe, requisicao));
    }
}
