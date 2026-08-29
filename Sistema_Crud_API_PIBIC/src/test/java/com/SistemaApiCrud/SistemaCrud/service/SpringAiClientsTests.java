package com.SistemaApiCrud.SistemaCrud.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.ConnectException;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;

import com.SistemaApiCrud.SistemaCrud.exception.ServicoIndisponivelException;

class SpringAiClientsTests {

    @Test
    void deveTraduzirGatewayIndisponivelAoGerarCasoClinico() {
        ChatClient.Builder builder = prepararBuilder();
        ControleUsoIa controleUso = controleComConexaoRecusada();
        SpringAiCasoClinicoClient client = new SpringAiCasoClinicoClient(builder, controleUso);

        assertThatThrownBy(() -> client.gerarConteudoComMetricas("sistema", "contexto"))
                .isInstanceOf(ServicoIndisponivelException.class)
                .hasMessageContaining("gateway de IA esta indisponivel")
                .hasRootCauseInstanceOf(ConnectException.class);
    }

    @Test
    void deveTraduzirGatewayIndisponivelAoGerarPerguntas() {
        ChatClient.Builder builder = prepararBuilder();
        ControleUsoIa controleUso = controleComConexaoRecusada();
        SpringAiPerguntaClient client = new SpringAiPerguntaClient(builder, controleUso);

        assertThatThrownBy(() -> client.gerarPerguntasComMetricas("sistema", "contexto"))
                .isInstanceOf(ServicoIndisponivelException.class)
                .hasMessageContaining("gateway de IA esta indisponivel")
                .hasRootCauseInstanceOf(ConnectException.class);
    }

    private ChatClient.Builder prepararBuilder() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.defaultAdvisors(any(Advisor[].class))).thenReturn(builder);
        when(builder.build()).thenReturn(mock(ChatClient.class));
        return builder;
    }

    private ControleUsoIa controleComConexaoRecusada() {
        ControleUsoIa controleUso = mock(ControleUsoIa.class);
        doThrow(new RuntimeException(new ConnectException("Connection refused")))
                .when(controleUso)
                .executar(any());
        return controleUso;
    }
}
