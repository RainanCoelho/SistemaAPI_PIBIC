package com.SistemaApiCrud.SistemaCrud.service;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.SistemaApiCrud.SistemaCrud.DTO.CasoClinicoRequestDTO;
import com.SistemaApiCrud.SistemaCrud.DTO.CasoClinicoResponseDTO;
import com.SistemaApiCrud.SistemaCrud.DTO.ChatRequestDTO;
import com.SistemaApiCrud.SistemaCrud.DTO.MessageDTO;
import com.SistemaApiCrud.SistemaCrud.entity.casos_clinicos;
import com.SistemaApiCrud.SistemaCrud.entity.conteudo_clinico;
import com.SistemaApiCrud.SistemaCrud.exception.BusinessException;
import com.SistemaApiCrud.SistemaCrud.exception.RecursoNaoEncontradoException;
import com.SistemaApiCrud.SistemaCrud.repository.caso_clinico_repository;
import com.SistemaApiCrud.SistemaCrud.repository.conteudo_clinico_repository;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
public class GroqService {

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final Gson gson;
    private final caso_clinico_repository casoRepository;
    private final conteudo_clinico_repository conteudoRepository;
    private final String apiKey;
    private final String model;

    public GroqService(
            OkHttpClient client,
            Gson gson,
            caso_clinico_repository casoRepository,
            conteudo_clinico_repository conteudoRepository,
            @Value("${groq.api.key:}") String apiKey,
            @Value("${groq.model:llama-3.3-70b-versatile}") String model) {
        this.client = client;
        this.gson = gson;
        this.casoRepository = casoRepository;
        this.conteudoRepository = conteudoRepository;
        this.apiKey = apiKey;
        this.model = model;
    }

    public CasoClinicoResponseDTO gerarConteudo(Long idCaso, CasoClinicoRequestDTO dto) {
        casos_clinicos caso = casoRepository.findById(idCaso)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Caso clinico nao encontrado"));

        JsonObject conteudoGerado = precisaGerar(dto)
                ? gerarCamposComIa(caso, dto)
                : new JsonObject();

        CasoClinicoResponseDTO response = montarResponse(idCaso, dto, conteudoGerado);
        conteudo_clinico conteudoSalvo = salvarConteudo(caso, response);
        response.setIdConteudo(conteudoSalvo.getIdConteudo());

        return response;
    }

    private JsonObject gerarCamposComIa(casos_clinicos caso, CasoClinicoRequestDTO dto) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException("Configure a variavel GROQ_API_KEY antes de gerar conteudo com IA");
        }

        String respostaBruta = chamarGroq(montarPrompt(caso, dto));
        String conteudo = extrairConteudoDaResposta(respostaBruta);

        try {
            JsonElement json = gson.fromJson(conteudo, JsonElement.class);
            if (json == null || !json.isJsonObject()) {
                throw new BusinessException("A IA retornou um conteudo em formato invalido");
            }
            return json.getAsJsonObject();
        } catch (JsonParseException ex) {
            throw new BusinessException("A IA retornou um JSON invalido");
        }
    }

    private CasoClinicoResponseDTO montarResponse(Long idCaso, CasoClinicoRequestDTO dto, JsonObject gerado) {
        CasoClinicoResponseDTO response = new CasoClinicoResponseDTO();
        response.setIdCaso(idCaso);
        response.setSintomas(campoFinal(dto.getSintomas(), gerado, "sintomas"));
        response.setContexto(campoFinal(dto.getContexto(), gerado, "contexto"));
        response.setExamClinico(campoFinal(dto.getExamClinico(), gerado, "examClinico"));
        response.setAntecClinico(campoFinal(dto.getAntecClinico(), gerado, "antecClinico"));
        response.setDiagEsperado(campoFinal(dto.getDiagEsperado(), gerado, "diagEsperado"));
        return response;
    }

    private conteudo_clinico salvarConteudo(casos_clinicos caso, CasoClinicoResponseDTO response) {
        conteudo_clinico conteudo = new conteudo_clinico();
        conteudo.setCasoClinico(caso);
        conteudo.setSintomas(response.getSintomas());
        conteudo.setContexto(response.getContexto());
        conteudo.setExamClinico(response.getExamClinico());
        conteudo.setAntecClinico(response.getAntecClinico());
        conteudo.setDiagEsperado(response.getDiagEsperado());
        return conteudoRepository.save(conteudo);
    }

    private String montarPrompt(casos_clinicos caso, CasoClinicoRequestDTO dto) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Voce e um assistente medico educacional. ");
        prompt.append("Gere um caso clinico para a area de ").append(caso.getAreaSaude());
        prompt.append(", especialidade ").append(caso.getEspecialidade());
        prompt.append(", dificuldade ").append(caso.getDificuldade());
        prompt.append(", estilo ").append(caso.getEstilo()).append(". ");

        if (preenchido(caso.getObjetivoAprendizagem())) {
            prompt.append("Objetivo de aprendizagem: ").append(caso.getObjetivoAprendizagem()).append(". ");
        }

        prompt.append("Os seguintes campos ja foram preenchidos pelo professor e devem ser mantidos exatamente como estao: ");
        adicionarCampoInformado(prompt, "Sintomas", dto.getSintomas());
        adicionarCampoInformado(prompt, "Contexto", dto.getContexto());
        adicionarCampoInformado(prompt, "Exame clinico", dto.getExamClinico());
        adicionarCampoInformado(prompt, "Antecedentes clinicos", dto.getAntecClinico());
        adicionarCampoInformado(prompt, "Diagnostico esperado", dto.getDiagEsperado());

        prompt.append("Gere o conteudo apenas para os campos que nao foram preenchidos. ");
        prompt.append("Responda somente em JSON valido, sem markdown e sem texto adicional. ");
        prompt.append("Inclua todos os campos neste formato exato: ");
        prompt.append("{\"sintomas\":\"\",\"contexto\":\"\",\"examClinico\":\"\",\"antecClinico\":\"\",\"diagEsperado\":\"\"}");
        return prompt.toString();
    }

    private void adicionarCampoInformado(StringBuilder prompt, String nome, String valor) {
        if (preenchido(valor)) {
            prompt.append(nome).append(": ").append(valor).append(". ");
        }
    }

    private String chamarGroq(String prompt) {
        ChatRequestDTO chatRequest = new ChatRequestDTO(model, List.of(new MessageDTO("user", prompt)));
        RequestBody requestBody = RequestBody.create(gson.toJson(chatRequest), JSON);

        Request request = new Request.Builder()
                .url(GROQ_URL)
                .header("Authorization", "Bearer " + apiKey)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (response.isSuccessful()) {
                return body;
            }
            throw new BusinessException("Erro ao chamar a Groq: status " + response.code());
        } catch (IOException ex) {
            throw new BusinessException("Nao foi possivel conectar com a Groq");
        }
    }

    private String extrairConteudoDaResposta(String respostaBruta) {
        try {
            JsonObject resposta = gson.fromJson(respostaBruta, JsonObject.class);
            JsonArray choices = resposta.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new BusinessException("A Groq nao retornou nenhuma resposta");
            }
            JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
            if (message == null || !message.has("content")) {
                throw new BusinessException("A resposta da Groq nao contem o conteudo esperado");
            }
            return message.get("content").getAsString();
        } catch (IllegalStateException | NullPointerException | JsonParseException ex) {
            throw new BusinessException("A resposta da Groq veio em formato inesperado");
        }
    }

    private String campoFinal(String informado, JsonObject gerado, String campo) {
        if (preenchido(informado)) {
            return informado;
        }

        if (!gerado.has(campo) || gerado.get(campo).isJsonNull() || !preenchido(gerado.get(campo).getAsString())) {
            throw new BusinessException("A IA nao retornou o campo obrigatorio: " + campo);
        }

        return gerado.get(campo).getAsString();
    }

    private boolean precisaGerar(CasoClinicoRequestDTO dto) {
        return !preenchido(dto.getSintomas())
                || !preenchido(dto.getContexto())
                || !preenchido(dto.getExamClinico())
                || !preenchido(dto.getAntecClinico())
                || !preenchido(dto.getDiagEsperado());
    }

    private boolean preenchido(String valor) {
        return valor != null && !valor.isBlank();
    }
}
