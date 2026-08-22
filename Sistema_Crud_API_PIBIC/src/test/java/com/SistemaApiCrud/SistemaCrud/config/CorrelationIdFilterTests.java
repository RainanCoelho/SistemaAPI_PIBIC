package com.SistemaApiCrud.SistemaCrud.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTests {

    private final CorrelationIdFilter filtro = new CorrelationIdFilter();

    @Test
    void devePreservarIdentificadorValidoEPropagarNaResposta() throws Exception {
        MockHttpServletRequest requisicao = new MockHttpServletRequest("GET", "/casos");
        requisicao.addHeader(CorrelationIdFilter.CABECALHO, "cliente-abc_123");
        MockHttpServletResponse resposta = new MockHttpServletResponse();

        filtro.doFilter(requisicao, resposta, new MockFilterChain());

        assertThat(requisicao.getAttribute(CorrelationIdFilter.ATRIBUTO))
                .isEqualTo("cliente-abc_123");
        assertThat(resposta.getHeader(CorrelationIdFilter.CABECALHO))
                .isEqualTo("cliente-abc_123");
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void deveSubstituirIdentificadorInvalidoPorUuid() throws Exception {
        MockHttpServletRequest requisicao = new MockHttpServletRequest("GET", "/casos");
        requisicao.addHeader(CorrelationIdFilter.CABECALHO, "valor com espacos");
        MockHttpServletResponse resposta = new MockHttpServletResponse();

        filtro.doFilter(requisicao, resposta, new MockFilterChain());

        String gerado = resposta.getHeader(CorrelationIdFilter.CABECALHO);
        assertThat(gerado)
                .isNotEqualTo("valor com espacos")
                .matches("[0-9a-f-]{36}");
    }
}
