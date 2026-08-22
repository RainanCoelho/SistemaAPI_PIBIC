package com.SistemaApiCrud.SistemaCrud.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtServiceTests {

    private static final String SECRET =
            "segredo-de-teste-com-mais-de-sessenta-e-quatro-caracteres-para-hs512-2026";
    private static final String ISSUER = "sistema-api-pibic";
    private static final String AUDIENCE = "clientes-pibic";

    private JwtService service;

    @BeforeEach
    void configurar() {
        service = new JwtService(SECRET, 30, ISSUER, AUDIENCE);
    }

    @Test
    void deveEmitirEValidarTokenComClaimsObrigatorias() {
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "professor",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_PROFESSOR")));

        String token = service.gerarToken(authentication);

        assertThat(service.isTokenValido(token)).isTrue();
        assertThat(service.getUsername(token)).isEqualTo("professor");
        assertThat(service.getExpiraEm(token)).isAfter(Instant.now());
        assertThat(service.criarAuthentication(token).getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_PROFESSOR");
    }

    @Test
    void deveRejeitarEmissorAudienciaNbfEAlgoritmoDivergentes() {
        Instant agora = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        assertThat(service.isTokenValido(token(
                "outro-emissor", List.of(AUDIENCE), agora, MacAlgorithm.HS256, true))).isFalse();
        assertThat(service.isTokenValido(token(
                ISSUER, List.of("outra-audiencia"), agora, MacAlgorithm.HS256, true))).isFalse();
        assertThat(service.isTokenValido(token(
                ISSUER, List.of(AUDIENCE), agora.plus(10, ChronoUnit.MINUTES),
                MacAlgorithm.HS256, true))).isFalse();
        assertThat(service.isTokenValido(token(
                ISSUER, List.of(AUDIENCE), agora, MacAlgorithm.HS512, true))).isFalse();
    }

    @Test
    void deveRejeitarTokenSemExpiracaoEOversized() {
        Instant agora = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        assertThat(service.isTokenValido(token(
                ISSUER, List.of(AUDIENCE), agora, MacAlgorithm.HS256, false))).isFalse();
        assertThat(service.isTokenValido("a".repeat(8_193))).isFalse();
    }

    private String token(
            String issuer,
            List<String> audience,
            Instant notBefore,
            MacAlgorithm algorithm,
            boolean incluirExpiracao) {
        Instant agora = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("professor")
                .audience(audience)
                .issuedAt(agora)
                .notBefore(notBefore)
                .claim("roles", List.of("ROLE_PROFESSOR"))
                .claim("ver", 0L);
        if (incluirExpiracao) {
            claims.expiresAt(agora.plus(30, ChronoUnit.MINUTES));
        }

        SecretKey key = new SecretKeySpec(
                SECRET.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");
        JwtEncoder encoder = NimbusJwtEncoder.withSecretKey(key)
                .algorithm(algorithm)
                .build();
        JwsHeader header = JwsHeader.with(algorithm).type("JWT").build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }
}
