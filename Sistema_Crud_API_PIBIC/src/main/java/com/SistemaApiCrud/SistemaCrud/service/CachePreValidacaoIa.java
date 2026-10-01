package com.SistemaApiCrud.SistemaCrud.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/** Guarda apenas hashes de contextos aprovados, nunca texto clinico ou saidas. */
final class CachePreValidacaoIa {

    private final Map<String, Long> aprovados = new LinkedHashMap<>();
    private static final int MAXIMO = 256;

    synchronized boolean aprovado(String contexto) {
        long agora = System.nanoTime();
        aprovados.values().removeIf(expiracao -> expiracao <= agora);
        return aprovados.containsKey(hash(contexto));
    }

    synchronized void aprovar(String contexto, Duration ttl) {
        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }
        if (aprovados.size() >= MAXIMO) {
            aprovados.remove(aprovados.keySet().iterator().next());
        }
        aprovados.put(hash(contexto), System.nanoTime() + ttl.toNanos());
    }

    private String hash(String contexto) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(contexto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponivel", ex);
        }
    }
}
