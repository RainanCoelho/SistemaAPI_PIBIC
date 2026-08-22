package com.SistemaApiCrud.SistemaCrud.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.SistemaApiCrud.SistemaCrud.exception.ApiProblemSupport;

import jakarta.servlet.http.HttpServletRequestWrapper;

class FiltroLimiteCorpoRequisicaoTests {

    @Test
    void deveRejeitarTamanhoDeclaradoAcimaDoLimite() throws Exception {
        FiltroLimiteCorpoRequisicao filtro = filtro(10);
        MockHttpServletRequest requisicao = requisicaoComCorpo("conteudo maior que dez");
        MockHttpServletResponse resposta = new MockHttpServletResponse();

        filtro.doFilter(requisicao, resposta, new MockFilterChain());

        assertThat(resposta.getStatus()).isEqualTo(413);
        assertThat(resposta.getContentType()).startsWith("application/problem+json");
        assertThat(resposta.getContentAsString()).contains("excede o limite");
    }

    @Test
    void deveRejeitarCorpoSemTamanhoDeclaradoAcimaDoLimite() throws Exception {
        FiltroLimiteCorpoRequisicao filtro = filtro(10);
        MockHttpServletRequest original = requisicaoComCorpo("conteudo maior que dez");
        HttpServletRequestWrapper requisicaoSemTamanho =
                new HttpServletRequestWrapper(original) {
                    @Override
                    public int getContentLength() {
                        return -1;
                    }

                    @Override
                    public long getContentLengthLong() {
                        return -1;
                    }
                };
        MockHttpServletResponse resposta = new MockHttpServletResponse();

        filtro.doFilter(requisicaoSemTamanho, resposta, new MockFilterChain());

        assertThat(resposta.getStatus()).isEqualTo(413);
    }

    @Test
    void deveRepassarCorpoPermitidoSemAlterarConteudo() throws Exception {
        FiltroLimiteCorpoRequisicao filtro = filtro(50);
        MockHttpServletRequest requisicao = requisicaoComCorpo("{\"campo\":\"valor\"}");
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        MockFilterChain cadeiaFiltros = new MockFilterChain();

        filtro.doFilter(requisicao, resposta, cadeiaFiltros);

        assertThat(resposta.getStatus()).isEqualTo(200);
        assertThat(cadeiaFiltros.getRequest()).isNotNull();
        assertThat(cadeiaFiltros.getRequest().getInputStream().readAllBytes())
                .asString(StandardCharsets.UTF_8)
                .isEqualTo("{\"campo\":\"valor\"}");
    }

    private MockHttpServletRequest requisicaoComCorpo(String corpo) {
        MockHttpServletRequest requisicao = new MockHttpServletRequest();
        requisicao.setMethod("POST");
        requisicao.setCharacterEncoding(StandardCharsets.UTF_8.name());
        requisicao.setContentType("application/json");
        requisicao.setContent(corpo.getBytes(StandardCharsets.UTF_8));
        return requisicao;
    }

    private FiltroLimiteCorpoRequisicao filtro(int limite) {
        return new FiltroLimiteCorpoRequisicao(new ApiProblemSupport(), limite);
    }
}
