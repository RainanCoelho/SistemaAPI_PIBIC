package com.SistemaApiCrud.SistemaCrud.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.SistemaApiCrud.SistemaCrud.DTO.CasoClinicoGeradoIaDTO;
import com.SistemaApiCrud.SistemaCrud.exception.BusinessException;

@Service
public class SpringAiCasoClinicoClient implements CasoClinicoAiClient {

    private final ChatClient chatClient;

    public SpringAiCasoClinicoClient(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public CasoClinicoGeradoIaDTO gerarConteudo(String prompt) {
        try {
            CasoClinicoGeradoIaDTO conteudo = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .entity(CasoClinicoGeradoIaDTO.class, spec -> spec.validateSchema());

            if (conteudo == null) {
                throw new BusinessException("A IA retornou um conteudo em formato invalido");
            }
            return conteudo;
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new BusinessException("Nao foi possivel gerar conteudo com Spring AI", ex);
        }
    }
}
