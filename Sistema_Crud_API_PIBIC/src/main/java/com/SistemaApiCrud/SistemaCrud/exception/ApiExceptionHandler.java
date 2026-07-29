package com.SistemaApiCrud.SistemaCrud.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> tratarRecursoNaoEncontrado(RecursoNaoEncontradoException ex) {
        return Map.of("erro", ex.getMessage());
    }

    @ExceptionHandler({BadRequestException.class, BusinessException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> tratarRequisicaoInvalida(RuntimeException ex) {
        return Map.of("erro", ex.getMessage());
    }

    @ExceptionHandler(ConflitoEstadoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> tratarConflitoDeEstado(ConflitoEstadoException ex) {
        return Map.of("erro", ex.getMessage());
    }

    @ExceptionHandler(AiProviderException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Map<String, String> tratarFalhaDoProvedorIa(AiProviderException ex) {
        return Map.of("erro", ex.getMessage());
    }

    @ExceptionHandler(ServicoIndisponivelException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, String> tratarServicoIndisponivel(ServicoIndisponivelException ex) {
        return Map.of("erro", ex.getMessage());
    }

    @ExceptionHandler(CapacidadeIaEsgotadaException.class)
    public ResponseEntity<Map<String, String>> tratarCapacidadeIaEsgotada(
            CapacidadeIaEsgotadaException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getSegundosAteNovaTentativa()))
                .body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> tratarAutenticacao() {
        return Map.of("erro", "Credenciais invalidas");
    }

    @ExceptionHandler(MuitasTentativasLoginException.class)
    public ResponseEntity<Map<String, String>> tratarMuitasTentativas(MuitasTentativasLoginException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(LimiteUsoIaException.class)
    public ResponseEntity<Map<String, String>> tratarLimiteUsoIa(LimiteUsoIaException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getSegundosAteNovaTentativa()))
                .body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(TempoEsgotadoIaException.class)
    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    public Map<String, String> tratarTempoEsgotadoIa(TempoEsgotadoIaException ex) {
        return Map.of("erro", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> tratarAcessoNegado() {
        return Map.of("erro", "Acesso negado");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> tratarValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();

        ex.getBindingResult().getFieldErrors()
                .forEach(error -> campos.put(error.getField(), error.getDefaultMessage()));

        return Map.of(
                "erro", "Dados invalidos",
                "campos", campos);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> tratarViolacaoDeRestricao(ConstraintViolationException ex) {
        Map<String, String> campos = new LinkedHashMap<>();

        ex.getConstraintViolations()
                .forEach(error -> campos.put(error.getPropertyPath().toString(), error.getMessage()));

        return Map.of(
                "erro", "Dados invalidos",
                "campos", campos);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> tratarIntegridadeDeDados() {
        return Map.of("erro", "Nao foi possivel concluir a operacao por causa de dados relacionados");
    }
}
