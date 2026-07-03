package com.SistemaApiCrud.SistemaCrud.service;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.SistemaApiCrud.SistemaCrud.DTO.CasoClinicoAjusteRequestDTO;
import com.SistemaApiCrud.SistemaCrud.DTO.CasoClinicoRequestDTO;
import com.SistemaApiCrud.SistemaCrud.DTO.CasoClinicoResponseDTO;
import com.SistemaApiCrud.SistemaCrud.DTO.ChatRequestDTO;
import com.SistemaApiCrud.SistemaCrud.DTO.MessageDTO;
import com.SistemaApiCrud.SistemaCrud.entity.casos_clinicos;
import com.SistemaApiCrud.SistemaCrud.entity.conteudo_clinico;
import com.SistemaApiCrud.SistemaCrud.entity.paciente;
import com.SistemaApiCrud.SistemaCrud.entity.enums.EstadoCivil;
import com.SistemaApiCrud.SistemaCrud.entity.enums.Sexo;
import com.SistemaApiCrud.SistemaCrud.exception.BusinessException;
import com.SistemaApiCrud.SistemaCrud.exception.RecursoNaoEncontradoException;
import com.SistemaApiCrud.SistemaCrud.repository.caso_clinico_repository;
import com.SistemaApiCrud.SistemaCrud.repository.conteudo_clinico_repository;
import com.SistemaApiCrud.SistemaCrud.repository.paciente_repository;
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
    private final paciente_repository pacienteRepository;
    private final String apiKey;
    private final String model;

    public GroqService(
            OkHttpClient client,
            Gson gson,
            caso_clinico_repository casoRepository,
            conteudo_clinico_repository conteudoRepository,
            paciente_repository pacienteRepository,
            @Value("${groq.api.key:}") String apiKey,
            @Value("${groq.model:llama-3.3-70b-versatile}") String model) {
        this.client = client;
        this.gson = gson;
        this.casoRepository = casoRepository;
        this.conteudoRepository = conteudoRepository;
        this.pacienteRepository = pacienteRepository;
        this.apiKey = apiKey;
        this.model = model;
    }

    public CasoClinicoResponseDTO gerarConteudo(Long idCaso, CasoClinicoRequestDTO dto) {
        casos_clinicos caso = casoRepository.findById(idCaso)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Caso clinico nao encontrado"));

        JsonObject conteudoGerado = precisaGerar(caso, dto)
                ? gerarCamposComIa(caso, dto)
                : new JsonObject();

        aplicarComplementosGerados(caso, dto, conteudoGerado);

        CasoClinicoResponseDTO response = montarResponse(idCaso, dto, conteudoGerado);
        conteudo_clinico conteudoSalvo = salvarConteudo(caso, response);
        response.setIdConteudo(conteudoSalvo.getIdConteudo());

        return response;
    }

    public CasoClinicoResponseDTO ajustarConteudo(Long idCaso, CasoClinicoAjusteRequestDTO dto) {
        casos_clinicos caso = casoRepository.findById(idCaso)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Caso clinico nao encontrado"));
        conteudo_clinico conteudoAtual = conteudoRepository.findFirstByCasoClinicoIdCasoOrderByIdConteudoDesc(idCaso)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conteudo clinico nao encontrado para este caso"));

        JsonObject conteudoGerado = gerarJsonComPrompt(montarPromptAjuste(caso, conteudoAtual, dto));
        aplicarAjustesGerados(caso, conteudoGerado);
        CasoClinicoResponseDTO response = montarResponse(
                idCaso,
                new CasoClinicoRequestDTO(null, null, null, null, null),
                conteudoGerado);
        conteudo_clinico conteudoSalvo = atualizarConteudo(conteudoAtual, response);
        response.setIdConteudo(conteudoSalvo.getIdConteudo());

        return response;
    }

    private JsonObject gerarCamposComIa(casos_clinicos caso, CasoClinicoRequestDTO dto) {
        return gerarJsonComPrompt(montarPrompt(caso, dto));
    }

    private JsonObject gerarJsonComPrompt(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException("Configure a variavel GROQ_API_KEY antes de gerar conteudo com IA");
        }

        String respostaBruta = chamarGroq(prompt);
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
        return preencherESalvarConteudo(conteudo, response);
    }

    private conteudo_clinico atualizarConteudo(conteudo_clinico conteudo, CasoClinicoResponseDTO response) {
        return preencherESalvarConteudo(conteudo, response);
    }

    private conteudo_clinico preencherESalvarConteudo(conteudo_clinico conteudo, CasoClinicoResponseDTO response) {
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
            prompt.append("Mantenha esse objetivo exatamente como esta. ");
        } else {
            prompt.append("Gere um objetivo de aprendizagem objetivo e coerente com o caso. ");
        }

        pacienteRepository.findByCasoClinicoIdCaso(caso.getIdCaso()).stream()
                .findFirst()
                .ifPresent(paciente -> adicionarPacienteAoPrompt(
                        prompt,
                        paciente,
                        Boolean.TRUE.equals(dto.getPermitirComplementoIa())));

        adicionarRegrasDeComplementoAoPrompt(prompt, dto);

        prompt.append("Os seguintes campos ja foram preenchidos pelo professor e devem ser mantidos exatamente como estao: ");
        adicionarCampoInformado(prompt, "Sintomas", dto.getSintomas());
        adicionarCampoInformado(prompt, "Contexto", dto.getContexto());
        adicionarCampoInformado(prompt, "Exame clinico", dto.getExamClinico());
        adicionarCampoInformado(prompt, "Antecedentes clinicos", dto.getAntecClinico());
        adicionarCampoInformado(prompt, "Diagnostico esperado", dto.getDiagEsperado());

        prompt.append("Gere o conteudo apenas para os campos que nao foram preenchidos. ");
        prompt.append("Se objetivoAprendizagem nao existir no caso, gere esse campo tambem. ");
        if (Boolean.TRUE.equals(dto.getPermitirComplementoIa())) {
            prompt.append("Inclua o objeto paciente com valores completos para os dados ausentes. ");
            prompt.append("Use sexo apenas como MASCULINO, FEMININO, OUTRO ou NAO_INFORMADO. ");
            prompt.append("Use estadoCivil apenas como SOLTEIRO, CASADO, DIVORCIADO, VIUVO, SEPARADO, UNIAO_ESTAVEL ou NAO_INFORMADO. ");
        }
        prompt.append("Responda somente em JSON valido, sem markdown e sem texto adicional. ");
        prompt.append("Inclua todos os campos neste formato exato: ");
        prompt.append("{\"sintomas\":\"\",\"contexto\":\"\",\"examClinico\":\"\",\"antecClinico\":\"\",\"diagEsperado\":\"\",\"objetivoAprendizagem\":\"\",\"paciente\":{\"nome\":\"\",\"idade\":0,\"sexo\":\"NAO_INFORMADO\",\"estadoCivil\":\"NAO_INFORMADO\",\"profissao\":\"\",\"peso\":\"\",\"altura\":\"\"}}");
        return prompt.toString();
    }

    private void aplicarComplementosGerados(casos_clinicos caso, CasoClinicoRequestDTO dto, JsonObject gerado) {
        atualizarObjetivoAprendizagem(caso, gerado);

        if (Boolean.TRUE.equals(dto.getPermitirComplementoIa())) {
            pacienteRepository.findByCasoClinicoIdCaso(caso.getIdCaso()).stream()
                    .findFirst()
                    .ifPresent(paciente -> atualizarPacienteComIa(paciente, gerado, false));
        }
    }

    private void aplicarAjustesGerados(casos_clinicos caso, JsonObject gerado) {
        atualizarObjetivoAprendizagem(caso, gerado, true);
        pacienteRepository.findByCasoClinicoIdCaso(caso.getIdCaso()).stream()
                .findFirst()
                .ifPresent(paciente -> atualizarPacienteComIa(paciente, gerado, true));
    }

    private void atualizarObjetivoAprendizagem(casos_clinicos caso, JsonObject gerado) {
        atualizarObjetivoAprendizagem(caso, gerado, false);
    }

    private void atualizarObjetivoAprendizagem(casos_clinicos caso, JsonObject gerado, boolean sobrescreverValorAtual) {
        if (!sobrescreverValorAtual && preenchido(caso.getObjetivoAprendizagem())) {
            return;
        }

        String objetivoAprendizagem = textoDoJson(gerado, "objetivoAprendizagem");
        if (preenchido(objetivoAprendizagem)) {
            caso.setObjetivoAprendizagem(objetivoAprendizagem.trim());
            casoRepository.save(caso);
        }
    }

    private void atualizarPacienteComIa(paciente paciente, JsonObject gerado, boolean sobrescreverDadosAtuais) {
        JsonObject pacienteGerado = objetoDoJson(gerado, "paciente");
        if (pacienteGerado == null) {
            return;
        }

        boolean alterou = false;
        if (sobrescreverDadosAtuais || !valorPacienteInformado(paciente.getNome())) {
            alterou |= atualizarTextoPaciente(textoDoJson(pacienteGerado, "nome"), paciente::setNome);
        }
        if (sobrescreverDadosAtuais || paciente.getIdade() == null || paciente.getIdade() == 0) {
            Integer idadeGerada = inteiroDoJson(pacienteGerado, "idade");
            if (idadeGerada != null && idadeGerada > 0 && idadeGerada <= 130) {
                paciente.setIdade(idadeGerada);
                alterou = true;
            }
        }
        if (sobrescreverDadosAtuais || paciente.getSexo() == null || paciente.getSexo() == Sexo.NAO_INFORMADO) {
            Sexo sexoGerado = enumDoJson(pacienteGerado, "sexo", Sexo.class);
            if (sexoGerado != null) {
                paciente.setSexo(sexoGerado);
                alterou = true;
            }
        }
        if (sobrescreverDadosAtuais || paciente.getEstadoCivil() == null || paciente.getEstadoCivil() == EstadoCivil.NAO_INFORMADO) {
            EstadoCivil estadoCivilGerado = enumDoJson(pacienteGerado, "estadoCivil", EstadoCivil.class);
            if (estadoCivilGerado != null) {
                paciente.setEstadoCivil(estadoCivilGerado);
                alterou = true;
            }
        }
        if (sobrescreverDadosAtuais || !valorPacienteInformado(paciente.getProfissao())) {
            alterou |= atualizarTextoPaciente(textoDoJson(pacienteGerado, "profissao"), paciente::setProfissao);
        }
        if (sobrescreverDadosAtuais || !valorPacienteInformado(paciente.getPeso())) {
            alterou |= atualizarTextoPaciente(textoDoJson(pacienteGerado, "peso"), paciente::setPeso);
        }
        if (sobrescreverDadosAtuais || !valorPacienteInformado(paciente.getAltura())) {
            alterou |= atualizarTextoPaciente(textoDoJson(pacienteGerado, "altura"), paciente::setAltura);
        }

        if (alterou) {
            pacienteRepository.save(paciente);
        }
    }

    private String montarPromptAjuste(
            casos_clinicos caso,
            conteudo_clinico conteudoAtual,
            CasoClinicoAjusteRequestDTO ajuste) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Voce e um assistente medico educacional. ");
        prompt.append("Reescreva o conteudo clinico de um caso existente para a area de ").append(caso.getAreaSaude());
        prompt.append(", especialidade ").append(caso.getEspecialidade());
        prompt.append(", dificuldade ").append(caso.getDificuldade());
        prompt.append(", estilo ").append(caso.getEstilo()).append(". ");

        if (preenchido(caso.getObjetivoAprendizagem())) {
            prompt.append("Objetivo de aprendizagem: ").append(caso.getObjetivoAprendizagem()).append(". ");
        }

        pacienteRepository.findByCasoClinicoIdCaso(caso.getIdCaso()).stream()
                .findFirst()
                .ifPresent(paciente -> adicionarPacienteAoPrompt(prompt, paciente, false));

        prompt.append("Conteudo atual do caso: ");
        adicionarCampoInformado(prompt, "Sintomas", conteudoAtual.getSintomas());
        adicionarCampoInformado(prompt, "Contexto", conteudoAtual.getContexto());
        adicionarCampoInformado(prompt, "Exame clinico", conteudoAtual.getExamClinico());
        adicionarCampoInformado(prompt, "Antecedentes clinicos", conteudoAtual.getAntecClinico());
        adicionarCampoInformado(prompt, "Diagnostico esperado", conteudoAtual.getDiagEsperado());

        prompt.append("Ajuste solicitado: ").append(instrucaoPorTipo(ajuste)).append(" ");
        prompt.append("Se o ajuste solicitado alterar dados do paciente, como idade, sexo, estado civil, profissao, peso, altura ou nome, atualize o objeto paciente e reescreva todo o conteudo clinico de forma coerente com esses novos dados. ");
        prompt.append("Se o ajuste nao pedir mudanca no paciente, preserve o paciente atual no objeto paciente. ");
        prompt.append("Preserve a coerencia clinica, o diagnostico central quando fizer sentido e o objetivo educacional. ");
        prompt.append("Use sexo apenas como MASCULINO, FEMININO, OUTRO ou NAO_INFORMADO. ");
        prompt.append("Use estadoCivil apenas como SOLTEIRO, CASADO, DIVORCIADO, VIUVO, SEPARADO, UNIAO_ESTAVEL ou NAO_INFORMADO. ");
        prompt.append("Responda somente em JSON valido, sem markdown e sem texto adicional. ");
        prompt.append("Inclua todos os campos neste formato exato: ");
        prompt.append("{\"sintomas\":\"\",\"contexto\":\"\",\"examClinico\":\"\",\"antecClinico\":\"\",\"diagEsperado\":\"\",\"objetivoAprendizagem\":\"\",\"paciente\":{\"nome\":\"\",\"idade\":0,\"sexo\":\"NAO_INFORMADO\",\"estadoCivil\":\"NAO_INFORMADO\",\"profissao\":\"\",\"peso\":\"\",\"altura\":\"\"}}");
        return prompt.toString();
    }

    private String instrucaoPorTipo(CasoClinicoAjusteRequestDTO ajuste) {
        String tipo = ajuste.getTipoAjuste() != null ? ajuste.getTipoAjuste().trim().toUpperCase() : "";
        String instrucao = ajuste.getInstrucao() != null ? ajuste.getInstrucao().trim() : "";

        return switch (tipo) {
            case "REGERAR" -> "gere uma nova versao completa do caso, mantendo a mesma proposta educacional e os dados do paciente.";
            case "SIMPLIFICAR" -> "torne o caso mais simples, com linguagem mais direta, menos distratores e menor complexidade clinica.";
            case "COMPLEXIFICAR" -> "torne o caso mais complexo, com mais detalhes clinicos relevantes, raciocinio mais sofisticado e comorbidades coerentes.";
            case "PERSONALIZADO" -> preenchido(instrucao)
                    ? instrucao
                    : "ajuste o caso mantendo coerencia clinica e educacional.";
            default -> throw new BusinessException("Tipo de ajuste de IA invalido");
        };
    }

    private void adicionarRegrasDeComplementoAoPrompt(StringBuilder prompt, CasoClinicoRequestDTO dto) {
        if (Boolean.TRUE.equals(dto.getPermitirComplementoIa())) {
            prompt.append("O professor permitiu que a IA complemente informacoes ausentes ou marcadas como NAO_INFORMADO. ");
            prompt.append("Use essa permissao apenas para enriquecer o conteudo clinico com detalhes coerentes, sem alterar nem contradizer dados obrigatorios ja informados do paciente. ");
        } else {
            prompt.append("Nao complemente dados cadastrais ausentes do paciente; use apenas os dados informados pelo professor. ");
        }

        adicionarCampoInformado(
                prompt,
                "Informacoes adicionais do paciente fornecidas pelo professor",
                dto.getInformacoesAdicionaisPaciente());

        if (Boolean.TRUE.equals(dto.getIncluirResultadosExamesClinicos())) {
            prompt.append("Inclua resultados de exames clinicos, laboratoriais ou de imagem coerentes com o caso, principalmente no campo examClinico quando ele nao tiver sido preenchido pelo professor. ");
        } else {
            prompt.append("Se precisar gerar o campo examClinico, descreva achados do exame fisico e nao inclua resultados laboratoriais, de imagem ou exames complementares. ");
        }
    }

    private void adicionarPacienteAoPrompt(StringBuilder prompt, paciente paciente, boolean permitirComplemento) {
        if (permitirComplemento) {
            prompt.append("Dados do paciente informados pelo professor. ");
            prompt.append("Valores ausentes, NAO_INFORMADO ou idade 0 podem ser complementados pela IA sem contradizer os dados concretos: ");
        } else {
            prompt.append("Dados obrigatorios do paciente, que nao podem ser alterados nem contraditos: ");
        }

        boolean possuiDadoConcreto = false;
        possuiDadoConcreto |= adicionarDadoPaciente(prompt, "nome", paciente.getNome(), permitirComplemento);
        possuiDadoConcreto |= adicionarIdadePaciente(prompt, paciente.getIdade(), permitirComplemento);
        possuiDadoConcreto |= adicionarDadoPaciente(prompt, "sexo", valorComoTexto(paciente.getSexo()), permitirComplemento);
        possuiDadoConcreto |= adicionarDadoPaciente(prompt, "estado civil", valorComoTexto(paciente.getEstadoCivil()), permitirComplemento);
        possuiDadoConcreto |= adicionarDadoPaciente(prompt, "profissao", paciente.getProfissao(), permitirComplemento);
        possuiDadoConcreto |= adicionarDadoPaciente(prompt, "peso", paciente.getPeso(), permitirComplemento);
        possuiDadoConcreto |= adicionarDadoPaciente(prompt, "altura", paciente.getAltura(), permitirComplemento);

        if (!possuiDadoConcreto && permitirComplemento) {
            prompt.append("nenhum dado cadastral concreto foi informado. ");
        }

        prompt.append("Todo o conteudo gerado deve ser coerente com os dados concretos do paciente. ");
    }

    private boolean adicionarDadoPaciente(
            StringBuilder prompt,
            String nome,
            String valor,
            boolean permitirComplemento) {
        if (permitirComplemento && !valorPacienteInformado(valor)) {
            return false;
        }

        prompt.append(nome).append(": ").append(valor).append("; ");
        return true;
    }

    private boolean adicionarIdadePaciente(StringBuilder prompt, Integer idade, boolean permitirComplemento) {
        if (permitirComplemento && (idade == null || idade == 0)) {
            return false;
        }

        prompt.append("idade: ").append(idade).append(" anos; ");
        return true;
    }

    private boolean valorPacienteInformado(String valor) {
        return preenchido(valor) && !"NAO_INFORMADO".equalsIgnoreCase(valor.trim());
    }

    private String valorComoTexto(Object valor) {
        return valor != null ? valor.toString() : "NAO_INFORMADO";
    }

    private boolean atualizarTextoPaciente(String valorGerado, Consumer<String> atualizarCampo) {
        if (!valorPacienteInformado(valorGerado)) {
            return false;
        }

        atualizarCampo.accept(valorGerado.trim());
        return true;
    }

    private JsonObject objetoDoJson(JsonObject json, String campo) {
        if (json == null || !json.has(campo) || json.get(campo).isJsonNull() || !json.get(campo).isJsonObject()) {
            return null;
        }

        return json.getAsJsonObject(campo);
    }

    private String textoDoJson(JsonObject json, String campo) {
        if (json == null || !json.has(campo) || json.get(campo).isJsonNull() || !json.get(campo).isJsonPrimitive()) {
            return null;
        }

        return json.get(campo).getAsString();
    }

    private Integer inteiroDoJson(JsonObject json, String campo) {
        String valor = textoDoJson(json, campo);
        if (!preenchido(valor)) {
            return null;
        }

        try {
            return Integer.valueOf(valor.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private <T extends Enum<T>> T enumDoJson(JsonObject json, String campo, Class<T> tipoEnum) {
        String valor = textoDoJson(json, campo);
        if (!valorPacienteInformado(valor)) {
            return null;
        }

        try {
            return Enum.valueOf(tipoEnum, valor.trim().toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException ex) {
            return null;
        }
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

    private boolean precisaGerar(casos_clinicos caso, CasoClinicoRequestDTO dto) {
        return !preenchido(dto.getSintomas())
                || !preenchido(dto.getContexto())
                || !preenchido(dto.getExamClinico())
                || !preenchido(dto.getAntecClinico())
                || !preenchido(dto.getDiagEsperado())
                || !preenchido(caso.getObjetivoAprendizagem())
                || Boolean.TRUE.equals(dto.getPermitirComplementoIa());
    }

    private boolean preenchido(String valor) {
        return valor != null && !valor.isBlank();
    }
}
