package com.SistemaApiCrud.SistemaCrud.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
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
    void usaContratoCompactoNaCoerenciaSemGerarCamposClinicos() {
        var model = mock(org.springframework.ai.chat.model.ChatModel.class);
        when(model.getOptions()).thenReturn(org.springframework.ai.chat.prompt.ChatOptions.builder().build());
        var controle = mock(ControleUsoIa.class);
        when(controle.executar(any())).thenAnswer(invocation -> {
            ControleUsoIa.OperacaoIa<?> operacao = invocation.getArgument(0);
            return operacao.executar();
        });
        when(model.call(any(org.springframework.ai.chat.prompt.Prompt.class))).thenReturn(
                new org.springframework.ai.chat.model.ChatResponse(java.util.List.of(
                        new org.springframework.ai.chat.model.Generation(
                                new org.springframework.ai.chat.messages.AssistantMessage(
                                        "{\"statusCoerencia\":\"COERENTE\",\"violacoes\":{}}")))));
        var client = new SpringAiCasoClinicoClient(ChatClient.builder(model), controle);
        assertThat(client.avaliarCoerencia("Avalie os dados", "Dados ficticios").entidade().getStatusCoerencia())
                .isEqualTo("COERENTE");
        var prompt = org.mockito.ArgumentCaptor.forClass(org.springframework.ai.chat.prompt.Prompt.class);
        org.mockito.Mockito.verify(model).call(prompt.capture());
        assertThat(prompt.getValue().getContents()).contains("statusCoerencia", "violacoes")
                .doesNotContain("sintomas", "examClinico", "objetivoAprendizagem");
    }

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
        when(builder.clone()).thenReturn(builder);
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
