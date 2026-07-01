package com.SistemaApiCrud.SistemaCrud.service;


import com.SistemaApiCrud.SistemaCrud.DTO.CasoClinicoRequestDTO;
import com.SistemaApiCrud.SistemaCrud.DTO.CasoClinicoResponseDTO;
import com.SistemaApiCrud.SistemaCrud.entity.casos_clinicos;
import com.SistemaApiCrud.SistemaCrud.entity.conteudo_clinico;
import com.SistemaApiCrud.SistemaCrud.repository.CasoClinicoRepository;
import com.SistemaApiCrud.SistemaCrud.repository.conteudo_clinico_repository;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class GroqService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.model}")
    private String model;

    private final OkHttpClient client;
    private final Gson gson;
    private final CasoClinicoRepository casoClinicoRepository;
    private final conteudo_clinico_repository conteudoRepository;

    public GroqService(OkHttpClient client, Gson gson,
                       CasoClinicoRepository casoClinicoRepository,
                       conteudo_clinico_repository conteudoRepository) {
        this.client = client;
        this.gson = gson;
        this.casoClinicoRepository = casoClinicoRepository;
        this.conteudoRepository = conteudoRepository;
    }

    public CasoClinicoResponseDTO gerarConteudo(Long idCaso, CasoClinicoRequestDTO dto) throws IOException {

        casos_clinicos caso = casoClinicoRepository.findById(idCaso)
                .orElseThrow(() -> new RuntimeException("Caso clínico não encontrado"));

        String prompt = montarPrompt(caso, dto);

        String respostaBruta = chamarGroq(prompt);

        String conteudoGerado = gson.fromJson(respostaBruta, JsonObject.class)
                .getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();

        JsonObject json = gson.fromJson(conteudoGerado, JsonObject.class);
        //Aqui fica a parte onde o professor escreve os dados que ele quer que a IA pegue, se estiver vazio pega o da IA
        CasoClinicoResponseDTO response = new CasoClinicoResponseDTO();
        response.setSintomas(dto.getSintomas() != null ? dto.getSintomas() : json.get("sintomas").getAsString());
        response.setContexto(dto.getContexto() != null ? dto.getContexto() : json.get("contexto").getAsString());
        response.setExamClinico(dto.getExamClinico() != null ? dto.getExamClinico() : json.get("examClinico").getAsString());
        response.setAntecClinico(dto.getAntecClinico() != null ? dto.getAntecClinico() : json.get("antecClinico").getAsString());
        response.setDiagEsperado(dto.getDiagEsperado() != null ? dto.getDiagEsperado() : json.get("diagEsperado").getAsString());

        //Salvar no banco de dados
        conteudo_clinico entidade = new conteudo_clinico();
        entidade.setCasoClinico(caso);
        entidade.setSintomas(response.getSintomas());
        entidade.setContexto(response.getContexto());
        entidade.setExamClinico(response.getExamClinico());
        entidade.setAntecClinico(response.getAntecClinico());
        entidade.setDiagEsperado(response.getDiagEsperado());

        conteudoRepository.save(entidade);

        return response;
    }

    private String montarPrompt(casos_clinicos caso, CasoClinicoRequestDTO dto) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Você é um assistente médico educacional. ");
        prompt.append("Gere um caso clínico para a área de ").append(caso.getAreaSaude());
        prompt.append(", especialidade ").append(caso.getEspecialidade());
        prompt.append(", dificuldade ").append(caso.getDificuldade());
        prompt.append(", estilo ").append(caso.getEstilo()).append(". ");

        if (caso.getObjetivoAprendizagem() != null) {
            prompt.append("Objetivo de aprendizagem: ").append(caso.getObjetivoAprendizagem()).append(". ");
        }

        prompt.append("Os seguintes campos já foram preenchidos pelo professor e devem ser mantidos exatamente como estão: ");
        if (dto.getSintomas() != null) prompt.append("Sintomas: ").append(dto.getSintomas()).append(". ");
        if (dto.getContexto() != null) prompt.append("Contexto: ").append(dto.getContexto()).append(". ");
        if (dto.getExamClinico() != null) prompt.append("Exame clínico: ").append(dto.getExamClinico()).append(". ");
        if (dto.getAntecClinico() != null) prompt.append("Antecedentes clínicos: ").append(dto.getAntecClinico()).append(". ");
        if (dto.getDiagEsperado() != null) prompt.append("Diagnóstico esperado: ").append(dto.getDiagEsperado()).append(". ");

        prompt.append("Gere o conteúdo apenas para os campos que NÃO foram preenchidos acima. ");
        prompt.append("Responda SOMENTE em JSON válido, sem nenhum texto adicional antes ou depois, ");
        prompt.append("incluindo TODOS os 5 campos abaixo (repita os que já foram preenchidos e gere os que faltam), ");
        prompt.append("no seguinte formato exato: ");
        prompt.append("{\"sintomas\": \"\", \"contexto\": \"\", \"examClinico\": \"\", \"antecClinico\": \"\", \"diagEsperado\": \"\"}");

        return prompt.toString();
    }

    private String chamarGroq(String prompt) throws IOException {

        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);

        JsonArray messages = new JsonArray();
        messages.add(message);

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.add("messages", messages);

        RequestBody requestBody = RequestBody.create(
                gson.toJson(body),
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                return response.body().string();
            } else {
                String erroBody = response.body() != null ? response.body().string() : "";
                throw new IOException("Erro na API Groq: " + response.code() + " - " + response.message() + " - " + erroBody);
            }
        }
    }
}






