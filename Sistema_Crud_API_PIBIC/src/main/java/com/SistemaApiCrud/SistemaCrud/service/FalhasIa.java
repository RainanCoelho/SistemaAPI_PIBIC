package com.SistemaApiCrud.SistemaCrud.service;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.nio.channels.UnresolvedAddressException;
import java.util.concurrent.TimeoutException;

public final class FalhasIa {

    private FalhasIa() {
    }

    public static boolean possuiTempoEsgotado(Throwable falha) {
        Throwable causaAtual = falha;
        while (causaAtual != null) {
            if (causaAtual instanceof SocketTimeoutException
                    || causaAtual instanceof HttpTimeoutException
                    || causaAtual instanceof TimeoutException
                    || causaAtual.getClass().getSimpleName().contains("Timeout")) {
                return true;
            }
            causaAtual = causaAtual.getCause();
        }
        return false;
    }

    public static boolean possuiLimiteDoProvedor(Throwable falha) {
        Throwable causaAtual = falha;
        while (causaAtual != null) {
            if ("RateLimitException".equals(causaAtual.getClass().getSimpleName())) {
                return true;
            }
            causaAtual = causaAtual.getCause();
        }
        return false;
    }

    public static boolean possuiIndisponibilidadeDeRede(Throwable falha) {
        Throwable causaAtual = falha;
        while (causaAtual != null) {
            if (causaAtual instanceof ConnectException
                    || causaAtual instanceof NoRouteToHostException
                    || causaAtual instanceof UnknownHostException
                    || causaAtual instanceof UnresolvedAddressException) {
                return true;
            }
            causaAtual = causaAtual.getCause();
        }
        return false;
    }
}
