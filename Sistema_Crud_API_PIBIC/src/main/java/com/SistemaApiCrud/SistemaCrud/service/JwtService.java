package com.SistemaApiCrud.SistemaCrud.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.SistemaApiCrud.SistemaCrud.entity.Usuario;

@Service
public class JwtService {

    private static final String ALGORITHM = "HmacSHA256";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.security.jwt.secret}")
    private String secret;

    @Value("${app.security.jwt.expiration-minutes:120}")
    private long expirationMinutes;

    public String gerarToken(Authentication authentication) {
        return gerarToken(authentication, 0L);
    }

    public String gerarToken(Authentication authentication, Usuario usuario) {
        long versaoCredencial = usuario.getVersaoCredencial() == null ? 0L : usuario.getVersaoCredencial();
        return gerarToken(authentication, versaoCredencial);
    }

    private String gerarToken(Authentication authentication, long versaoCredencial) {
        Instant agora = Instant.now();
        Instant expiraEm = agora.plus(expirationMinutes, ChronoUnit.MINUTES);

        List<String> roles = authentication.getAuthorities()
                .stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .toList();

        Map<String, Object> header = Map.of(
                "alg", "HS256",
                "typ", "JWT");

        Map<String, Object> payload = Map.of(
                "sub", authentication.getName(),
                "roles", roles,
                "ver", versaoCredencial,
                "iat", agora.getEpochSecond(),
                "exp", expiraEm.getEpochSecond());

        String headerBase64 = base64Url(toJson(header));
        String payloadBase64 = base64Url(toJson(payload));
        String conteudo = headerBase64 + "." + payloadBase64;

        return conteudo + "." + assinar(conteudo);
    }

    public boolean isTokenValido(String token) {
        try {
            Map<String, Object> claims = lerClaims(token);
            return assinaturaValida(token) && getExpiraEm(claims).isAfter(Instant.now());
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public Authentication criarAuthentication(String token) {
        Map<String, Object> claims = lerClaims(token);
        String username = (String) claims.get("sub");

        List<SimpleGrantedAuthority> authorities = getRoles(claims).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        return new UsernamePasswordAuthenticationToken(username, null, authorities);
    }

    public Authentication criarAuthentication(String token, Usuario usuario) {
        String username = getUsername(token);
        if (!usuario.getUsername().equals(username)) {
            throw new IllegalArgumentException("Token invalido");
        }

        return new UsernamePasswordAuthenticationToken(
                usuario.getUsername(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name())));
    }

    public String getUsername(String token) {
        Object subject = lerClaims(token).get("sub");
        if (subject instanceof String username && !username.isBlank()) {
            return username;
        }
        throw new IllegalArgumentException("Token sem usuario");
    }

    public long getVersaoCredencial(String token) {
        Object versao = lerClaims(token).get("ver");
        if (versao instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    public Instant getExpiraEm(String token) {
        return getExpiraEm(lerClaims(token));
    }

    private boolean assinaturaValida(String token) {
        String[] partes = token.split("\\.");
        if (partes.length != 3) {
            return false;
        }

        String conteudo = partes[0] + "." + partes[1];
        String assinaturaEsperada = assinar(conteudo);

        return MessageDigest.isEqual(
                assinaturaEsperada.getBytes(StandardCharsets.UTF_8),
                partes[2].getBytes(StandardCharsets.UTF_8));
    }

    private Map<String, Object> lerClaims(String token) {
        String[] partes = token.split("\\.");
        if (partes.length != 3) {
            throw new IllegalArgumentException("Token invalido");
        }

        try {
            byte[] payload = Base64.getUrlDecoder().decode(partes[1]);
            return objectMapper.readValue(payload, new TypeReference<>() {});
        } catch (Exception ex) {
            throw new IllegalArgumentException("Token invalido", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> getRoles(Map<String, Object> claims) {
        Object roles = claims.get("roles");
        if (roles instanceof List<?>) {
            return ((List<?>) roles).stream()
                    .map(String::valueOf)
                    .toList();
        }

        return List.of();
    }

    private Instant getExpiraEm(Map<String, Object> claims) {
        Object exp = claims.get("exp");
        if (exp instanceof Number number) {
            return Instant.ofEpochSecond(number.longValue());
        }

        throw new IllegalArgumentException("Token sem expiracao");
    }

    private byte[] toJson(Map<String, Object> valor) {
        try {
            return objectMapper.writeValueAsBytes(valor);
        } catch (Exception ex) {
            throw new IllegalStateException("Nao foi possivel gerar o token", ex);
        }
    }

    private String base64Url(byte[] valor) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(valor);
    }

    private String assinar(String conteudo) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return base64Url(mac.doFinal(conteudo.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Nao foi possivel assinar o token", ex);
        }
    }
}
