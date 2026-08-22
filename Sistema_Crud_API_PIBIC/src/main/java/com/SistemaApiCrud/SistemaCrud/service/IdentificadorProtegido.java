package com.SistemaApiCrud.SistemaCrud.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class IdentificadorProtegido {

    private static final String ALGORITMO = "HmacSHA256";
    private static final byte[] DOMINIO =
            "sistema-api-pibic:identificadores-persistentes:v1"
                    .getBytes(StandardCharsets.UTF_8);

    private final SecretKey chaveDerivada;

    public IdentificadorProtegido(@Value("${app.security.jwt.secret}") String segredoAplicacao) {
        if (segredoAplicacao == null || segredoAplicacao.isBlank()) {
            throw new IllegalArgumentException("O segredo para proteger identificadores deve ser informado");
        }
        this.chaveDerivada = new SecretKeySpec(
                hmac(
                        new SecretKeySpec(
                                segredoAplicacao.getBytes(StandardCharsets.UTF_8),
                                ALGORITMO),
                        DOMINIO),
                ALGORITMO);
    }

    public String gerar(String categoria, String valor) {
        String conteudo = categoria + ":" + valor;
        return HexFormat.of().formatHex(hmac(
                chaveDerivada,
                conteudo.getBytes(StandardCharsets.UTF_8)));
    }

    private byte[] hmac(SecretKey chave, byte[] conteudo) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(chave);
            return mac.doFinal(conteudo);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HMAC-SHA-256 indisponivel", ex);
        }
    }
}
