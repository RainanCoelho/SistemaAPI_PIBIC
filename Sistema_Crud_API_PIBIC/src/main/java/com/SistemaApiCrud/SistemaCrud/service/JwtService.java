package com.SistemaApiCrud.SistemaCrud.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import com.SistemaApiCrud.SistemaCrud.entity.Usuario;

@Service
public class JwtService {

    private static final int TAMANHO_MAXIMO_TOKEN = 8_192;

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final long expirationMinutes;
    private final String issuer;
    private final String audience;

    public JwtService(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.expiration-minutes:120}") long expirationMinutes,
            @Value("${app.security.jwt.issuer:sistema-api-pibic}") String issuer,
            @Value("${app.security.jwt.audience:sistema-api-pibic-clients}") String audience) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("O segredo JWT deve ser informado");
        }
        if (expirationMinutes < 1 || issuer.isBlank() || audience.isBlank()) {
            throw new IllegalArgumentException("A configuracao do JWT e invalida");
        }

        SecretKey key = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");
        this.encoder = NimbusJwtEncoder.withSecretKey(key)
                .algorithm(MacAlgorithm.HS256)
                .build();

        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new JwtClaimValidator<List<String>>("aud", audiencias ->
                        audiencias != null && audiencias.contains(audience)),
                new JwtClaimValidator<>("exp", Objects::nonNull),
                new JwtClaimValidator<>("nbf", Objects::nonNull),
                new JwtClaimValidator<String>("sub", subject ->
                        subject != null && !subject.isBlank())));
        this.decoder = jwtDecoder;
        this.expirationMinutes = expirationMinutes;
        this.issuer = issuer;
        this.audience = audience;
    }

    public String gerarToken(Authentication authentication) {
        return gerarToken(authentication, 0L);
    }

    public String gerarToken(Authentication authentication, Usuario usuario) {
        long versaoCredencial = usuario.getVersaoCredencial() == null
                ? 0L
                : usuario.getVersaoCredencial();
        return gerarToken(authentication, versaoCredencial);
    }

    private String gerarToken(Authentication authentication, long versaoCredencial) {
        Instant agora = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiraEm = agora.plus(expirationMinutes, ChronoUnit.MINUTES);
        List<String> roles = authentication.getAuthorities()
                .stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .toList();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .type("JWT")
                .build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(authentication.getName())
                .audience(List.of(audience))
                .issuedAt(agora)
                .notBefore(agora)
                .expiresAt(expiraEm)
                .claim("roles", roles)
                .claim("ver", versaoCredencial)
                .build();

        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public boolean isTokenValido(String token) {
        try {
            decodificar(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public Authentication criarAuthentication(String token) {
        Jwt jwt = decodificar(token);
        List<SimpleGrantedAuthority> authorities = getRoles(jwt).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new UsernamePasswordAuthenticationToken(jwt.getSubject(), null, authorities);
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
        String subject = decodificar(token).getSubject();
        if (subject != null && !subject.isBlank()) {
            return subject;
        }
        throw new IllegalArgumentException("Token sem usuario");
    }

    public long getVersaoCredencial(String token) {
        Object versao = decodificar(token).getClaim("ver");
        return versao instanceof Number number ? number.longValue() : 0L;
    }

    public Instant getExpiraEm(String token) {
        Instant expiraEm = decodificar(token).getExpiresAt();
        if (expiraEm == null) {
            throw new IllegalArgumentException("Token sem expiracao");
        }
        return expiraEm;
    }

    private Jwt decodificar(String token) {
        if (token == null || token.isBlank() || token.length() > TAMANHO_MAXIMO_TOKEN) {
            throw new IllegalArgumentException("Token invalido");
        }
        return decoder.decode(token);
    }

    private List<String> getRoles(Jwt jwt) {
        Object roles = jwt.getClaim("roles");
        if (roles instanceof List<?> lista) {
            return lista.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
