package com.SistemaApiCrud.SistemaCrud.service;

import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import com.SistemaApiCrud.SistemaCrud.dto.PerguntasGeradasIaDTO;
import com.SistemaApiCrud.SistemaCrud.exception.AiProviderException;
import com.SistemaApiCrud.SistemaCrud.exception.CapacidadeIaEsgotadaException;
import com.SistemaApiCrud.SistemaCrud.exception.LimiteUsoIaException;
import com.SistemaApiCrud.SistemaCrud.exception.TempoEsgotadoIaException;

@Service
public class SpringAiPerguntaClient implements PerguntaAiClient {

    private final ChatClient clienteConversa;
    private final ControleUsoIa controleUsoIa;

    public SpringAiPerguntaClient(
            ChatClient.Builder construtorClienteConversa,
            ControleUsoIa controleUsoIa) {
        this.clienteConversa = construtorClienteConversa
                .defaultAdvisors(StructuredOutputValidationAdvisor.builder()
                        .outputType(PerguntasGeradasIaDTO.class)
                        .maxRepeatAttempts(0)
                        .build())
                .build();
        this.controleUsoIa = controleUsoIa;
    }

    @Override
    public PerguntasGeradasIaDTO gerarPerguntas(String instrucoesSistema, String contexto) {
        return gerarPerguntasComMetricas(instrucoesSistema, contexto).entidade();
    }

    @Override
    public RespostaIaComMetricas<PerguntasGeradasIaDTO> gerarPerguntasComMetricas(
            String instrucoesSistema,
            String contexto) {
        try {
            long inicio = System.nanoTime();
            ResponseEntity<ChatResponse, PerguntasGeradasIaDTO> resposta = controleUsoIa.executar(() -> clienteConversa
                    .prompt()
                    .system(instrucoesSistema)
                    .user(contexto)
                    .call()
                    .responseEntity(PerguntasGeradasIaDTO.class));
            long duracaoMs = (System.nanoTime() - inicio) / 1_000_000L;
            PerguntasGeradasIaDTO perguntas = resposta == null ? null : resposta.entity();

            if (perguntas == null) {
                throw new AiProviderException("A IA retornou perguntas em formato invalido");
            }
            return comMetricas(perguntas, resposta == null ? null : resposta.response(), duracaoMs);
        } catch (AiProviderException
                | CapacidadeIaEsgotadaException
                | LimiteUsoIaException
                | TempoEsgotadoIaException falha) {
            throw falha;
        } catch (RuntimeException falha) {
            if (FalhasIa.possuiLimiteDoProvedor(falha)) {
                throw new CapacidadeIaEsgotadaException(
                        "Todos os provedores gratuitos de IA atingiram a capacidade disponivel",
                        60,
                        falha);
            }
            if (FalhasIa.possuiTempoEsgotado(falha)) {
                throw new TempoEsgotadoIaException(
                        "O provedor de IA excedeu o tempo limite da requisicao",
                        falha);
            }
            throw new AiProviderException("Nao foi possivel gerar perguntas com o provedor de IA", falha);
        }
    }

    private RespostaIaComMetricas<PerguntasGeradasIaDTO> comMetricas(
            PerguntasGeradasIaDTO perguntas,
            ChatResponse resposta,
            long duracaoMs) {
        ChatResponseMetadata metadados = resposta == null ? null : resposta.getMetadata();
        Usage uso = metadados == null ? null : metadados.getUsage();
        return new RespostaIaComMetricas<>(
                perguntas,
                duracaoMs,
                metadados == null ? null : metadados.getModel(),
                uso == null ? null : uso.getPromptTokens(),
                uso == null ? null : uso.getCompletionTokens());
    }
}
