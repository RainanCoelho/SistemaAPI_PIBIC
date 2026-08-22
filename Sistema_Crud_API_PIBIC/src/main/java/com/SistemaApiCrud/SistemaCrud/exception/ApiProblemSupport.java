package com.SistemaApiCrud.SistemaCrud.exception;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.SistemaApiCrud.SistemaCrud.config.CorrelationIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiProblemSupport {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public ApiProblem criar(
            HttpStatus status,
            String codigo,
            String titulo,
            String detalhe,
            HttpServletRequest requisicao) {
        return criar(status, codigo, titulo, detalhe, requisicao, Map.of());
    }

    public ApiProblem criar(
            HttpStatus status,
            String codigo,
            String titulo,
            String detalhe,
            HttpServletRequest requisicao,
            Map<String, String> erros) {
        Map<String, String> errosImutaveis = Collections.unmodifiableMap(
                new LinkedHashMap<>(erros));
        String correlationId = CorrelationIdFilter.obterOuCriar(requisicao);
        return new ApiProblem(
                "urn:sistema-api-pibic:problem:" + codigo,
                titulo,
                status.value(),
                detalhe,
                requisicao.getRequestURI(),
                Instant.now(),
                correlationId,
                detalhe,
                errosImutaveis,
                errosImutaveis);
    }

    public ResponseEntity<ApiProblem> responder(
            HttpStatus status,
            String codigo,
            String titulo,
            String detalhe,
            HttpServletRequest requisicao) {
        return responder(status, codigo, titulo, detalhe, requisicao, Map.of());
    }

    public ResponseEntity<ApiProblem> responder(
            HttpStatus status,
            String codigo,
            String titulo,
            String detalhe,
            HttpServletRequest requisicao,
            Map<String, String> erros) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(criar(status, codigo, titulo, detalhe, requisicao, erros));
    }

    public void escrever(
            HttpStatus status,
            String codigo,
            String titulo,
            String detalhe,
            HttpServletRequest requisicao,
            HttpServletResponse resposta) throws IOException {
        ApiProblem problema = criar(status, codigo, titulo, detalhe, requisicao);
        resposta.setStatus(status.value());
        resposta.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        resposta.setHeader(CorrelationIdFilter.CABECALHO, problema.correlationId());
        objectMapper.writeValue(resposta.getOutputStream(), problema);
    }
}
