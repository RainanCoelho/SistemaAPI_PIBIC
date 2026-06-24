package com.SistemaApiCrud.SistemaCrud.config;

import java.net.URI;
import java.util.Arrays;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionSecurityValidator implements InitializingBean {

    private final String jwtSecret;
    private final String allowedOrigins;

    public ProductionSecurityValidator(
            @Value("${app.security.jwt.secret}") String jwtSecret,
            @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.jwtSecret = jwtSecret;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void afterPropertiesSet() {
        if (jwtSecret == null || jwtSecret.length() < 64) {
            throw new IllegalStateException("JWT_SECRET deve ter pelo menos 64 caracteres em producao");
        }

        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS deve informar ao menos uma origem HTTPS");
        }

        Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .forEach(this::validarOrigem);
    }

    private void validarOrigem(String origin) {
        URI uri;
        try {
            uri = URI.create(origin);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Origem CORS invalida: " + origin, ex);
        }

        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new IllegalStateException("Origens CORS de producao devem usar HTTPS: " + origin);
        }

        if (uri.getUserInfo() != null
                || (uri.getPath() != null && !uri.getPath().isEmpty())
                || uri.getQuery() != null
                || uri.getFragment() != null) {
            throw new IllegalStateException("Origem CORS deve conter apenas esquema, host e porta: " + origin);
        }

        String host = uri.getHost();
        if ("localhost".equalsIgnoreCase(host) || host.startsWith("127.")) {
            throw new IllegalStateException("Origem local nao pode ser usada em producao: " + origin);
        }
    }
}
