package com.SistemaApiCrud.SistemaCrud.config;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.SistemaApiCrud.SistemaCrud.exception.ApiProblemSupport;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(0)
public class FiltroLimiteCorpoRequisicao extends OncePerRequestFilter {

    private static final Set<String> METODOS_COM_CORPO = Set.of("POST", "PUT", "PATCH");

    private final int limiteCorpoBytes;
    private final ApiProblemSupport problemas;

    public FiltroLimiteCorpoRequisicao(
            ApiProblemSupport problemas,
            @Value("${app.http.limite-corpo-bytes:1048576}") int limiteCorpoBytes) {
        if (limiteCorpoBytes < 1) {
            throw new IllegalArgumentException("O limite do corpo da requisicao deve ser maior que zero");
        }
        this.problemas = problemas;
        this.limiteCorpoBytes = limiteCorpoBytes;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest requisicao,
            HttpServletResponse resposta,
            FilterChain cadeiaFiltros) throws ServletException, IOException {
        if (!METODOS_COM_CORPO.contains(requisicao.getMethod())) {
            cadeiaFiltros.doFilter(requisicao, resposta);
            return;
        }

        long tamanhoDeclarado = requisicao.getContentLengthLong();
        if (tamanhoDeclarado > limiteCorpoBytes) {
            rejeitar(requisicao, resposta);
            return;
        }

        byte[] corpo = requisicao.getInputStream().readNBytes(limiteCorpoBytes + 1);
        if (corpo.length > limiteCorpoBytes) {
            rejeitar(requisicao, resposta);
            return;
        }

        cadeiaFiltros.doFilter(new RequisicaoComCorpo(requisicao, corpo), resposta);
    }

    private void rejeitar(
            HttpServletRequest requisicao,
            HttpServletResponse resposta) throws IOException {
        problemas.escrever(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "corpo-requisicao-excedido",
                "Corpo da requisicao excedido",
                "O corpo da requisicao excede o limite permitido",
                requisicao,
                resposta);
    }

    private static final class RequisicaoComCorpo extends HttpServletRequestWrapper {

        private final byte[] corpo;

        private RequisicaoComCorpo(HttpServletRequest requisicao, byte[] corpo) {
            super(requisicao);
            this.corpo = corpo;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream fluxo = new ByteArrayInputStream(corpo);
            return new ServletInputStream() {

                @Override
                public int read() {
                    return fluxo.read();
                }

                @Override
                public boolean isFinished() {
                    return fluxo.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener ouvinte) {
                    if (ouvinte == null) {
                        throw new IllegalArgumentException("O ouvinte de leitura nao pode ser nulo");
                    }
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            String codificacao = getCharacterEncoding();
            Charset conjuntoCaracteres = codificacao == null
                    ? StandardCharsets.UTF_8
                    : Charset.forName(codificacao);
            return new BufferedReader(new InputStreamReader(getInputStream(), conjuntoCaracteres));
        }

        @Override
        public int getContentLength() {
            return corpo.length;
        }

        @Override
        public long getContentLengthLong() {
            return corpo.length;
        }
    }
}
