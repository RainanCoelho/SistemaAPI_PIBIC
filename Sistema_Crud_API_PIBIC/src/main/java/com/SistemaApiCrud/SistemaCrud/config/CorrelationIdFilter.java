package com.SistemaApiCrud.SistemaCrud.config;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CABECALHO = "X-Correlation-Id";
    public static final String ATRIBUTO = CorrelationIdFilter.class.getName() + ".correlationId";
    private static final String CHAVE_MDC = "correlationId";
    private static final Pattern FORMATO_PERMITIDO =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,63}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest requisicao,
            HttpServletResponse resposta,
            FilterChain cadeiaFiltros) throws ServletException, IOException {
        String correlationId = normalizar(requisicao.getHeader(CABECALHO));
        requisicao.setAttribute(ATRIBUTO, correlationId);
        resposta.setHeader(CABECALHO, correlationId);
        MDC.put(CHAVE_MDC, correlationId);
        try {
            cadeiaFiltros.doFilter(requisicao, resposta);
        } finally {
            MDC.remove(CHAVE_MDC);
        }
    }

    public static String obterOuCriar(HttpServletRequest requisicao) {
        Object valor = requisicao.getAttribute(ATRIBUTO);
        if (valor instanceof String correlationId && !correlationId.isBlank()) {
            return correlationId;
        }
        String correlationId = UUID.randomUUID().toString();
        requisicao.setAttribute(ATRIBUTO, correlationId);
        return correlationId;
    }

    private String normalizar(String recebido) {
        if (recebido != null && FORMATO_PERMITIDO.matcher(recebido).matches()) {
            return recebido;
        }
        return UUID.randomUUID().toString();
    }
}
