package com.SistemaApiCrud.SistemaCrud.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.stereotype.Service;

import com.SistemaApiCrud.SistemaCrud.DTO.PerguntasGeradasIaDTO;
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
                        .maxRepeatAttempts(1)
                        .build())
                .build();
        this.controleUsoIa = controleUsoIa;
    }

    @Override
    public PerguntasGeradasIaDTO gerarPerguntas(String instrucoesSistema, String contexto) {
        try {
            PerguntasGeradasIaDTO perguntas = controleUsoIa.executar(() -> clienteConversa.prompt()
                    .system(instrucoesSistema)
                    .user(contexto)
                    .call()
                    .entity(PerguntasGeradasIaDTO.class));

            if (perguntas == null) {
                throw new AiProviderException("A IA retornou perguntas em formato invalido");
            }
            return perguntas;
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
}
