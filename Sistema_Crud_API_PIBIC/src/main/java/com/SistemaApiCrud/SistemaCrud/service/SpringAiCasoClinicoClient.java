package com.SistemaApiCrud.SistemaCrud.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.stereotype.Service;

import com.SistemaApiCrud.SistemaCrud.dto.CasoClinicoGeradoIaDTO;
import com.SistemaApiCrud.SistemaCrud.exception.AiProviderException;
import com.SistemaApiCrud.SistemaCrud.exception.CapacidadeIaEsgotadaException;
import com.SistemaApiCrud.SistemaCrud.exception.LimiteUsoIaException;
import com.SistemaApiCrud.SistemaCrud.exception.TempoEsgotadoIaException;

@Service
public class SpringAiCasoClinicoClient implements CasoClinicoAiClient {

    private final ChatClient clienteConversa;
    private final ControleUsoIa controleUsoIa;

    public SpringAiCasoClinicoClient(
            ChatClient.Builder construtorClienteConversa,
            ControleUsoIa controleUsoIa) {
        this.clienteConversa = construtorClienteConversa
                .defaultAdvisors(StructuredOutputValidationAdvisor.builder()
                        .outputType(CasoClinicoGeradoIaDTO.class)
                        .maxRepeatAttempts(1)
                        .build())
                .build();
        this.controleUsoIa = controleUsoIa;
    }

    @Override
    public CasoClinicoGeradoIaDTO gerarConteudo(String instrucoesSistema, String contexto) {
        try {
            CasoClinicoGeradoIaDTO conteudo = controleUsoIa.executar(() -> clienteConversa.prompt()
                    .system(instrucoesSistema)
                    .user(contexto)
                    .call()
                    .entity(CasoClinicoGeradoIaDTO.class));

            if (conteudo == null) {
                throw new AiProviderException("A IA retornou um conteudo em formato invalido");
            }
            return conteudo;
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
            throw new AiProviderException("Nao foi possivel gerar conteudo com o provedor de IA", falha);
        }
    }
}
