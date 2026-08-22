package com.SistemaApiCrud.SistemaCrud;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

class OpenApiContractTests {

    private static final String PACOTE_CONTROLLERS =
            "com.SistemaApiCrud.SistemaCrud.controller";
    private static final Set<String> METODOS_OPENAPI =
            Set.of("get", "post", "put", "patch", "delete", "head", "options", "trace");

    @Test
    void deveDocumentarTodasAsOperacoesDosControllersSemReferenciasQuebradas()
            throws Exception {
        Map<String, Object> documento = carregarOpenApi();

        assertThat(documento.get("openapi")).isEqualTo("3.0.3");
        assertThat(operacoesOpenApi(documento)).isEqualTo(operacoesControllers());

        List<String> referenciasQuebradas = new ArrayList<>();
        validarReferencias(documento, documento, referenciasQuebradas);
        assertThat(referenciasQuebradas).isEmpty();
    }

    @Test
    void deveDocumentarContratoPadraoDeProblemasEConfirmacoesDePrivacidade()
            throws Exception {
        Map<String, Object> documento = carregarOpenApi();
        Map<String, Object> schemas = mapa(mapa(documento.get("components")).get("schemas"));
        Map<String, Object> problem = mapa(schemas.get("Problem"));
        List<String> camposObrigatorios = lista(problem.get("required"));

        assertThat(camposObrigatorios).contains(
                "type", "title", "status", "detail", "instance", "timestamp",
                "correlationId");
        assertThat(propriedadeObrigatoria(schemas, "CasoClinicoIARequest",
                "dadosSinteticosOuDesidentificados")).isTrue();
        assertThat(propriedadeObrigatoria(schemas, "CasoClinicoIAAjusteRequest",
                "dadosSinteticosOuDesidentificados")).isTrue();
        assertThat(propriedadeObrigatoria(schemas, "GerarPerguntasIaRequestDTO",
                "dadosSinteticosOuDesidentificados")).isTrue();
        assertThat(propriedadeObrigatoria(schemas, "RevisarRespostaRequestDTO",
                "justificativa")).isTrue();

        Map<String, Object> respostas = mapa(mapa(documento.get("components")).get("responses"));
        respostas.values().forEach(resposta -> {
            Map<String, Object> content = mapa(mapa(resposta).get("content"));
            assertThat(content).containsKey("application/problem+json");
        });
    }

    private Map<String, Object> carregarOpenApi() throws Exception {
        LoaderOptions opcoes = new LoaderOptions();
        opcoes.setAllowDuplicateKeys(false);
        try (InputStream arquivo = getClass().getResourceAsStream("/static/openapi.yaml")) {
            assertThat(arquivo).as("arquivo openapi.yaml no classpath").isNotNull();
            Object documento = new Yaml(new SafeConstructor(opcoes)).load(arquivo);
            return mapa(documento);
        }
    }

    private Set<String> operacoesOpenApi(Map<String, Object> documento) {
        Set<String> operacoes = new LinkedHashSet<>();
        mapa(documento.get("paths")).forEach((caminho, item) ->
                mapa(item).keySet().stream()
                        .filter(METODOS_OPENAPI::contains)
                        .map(String::toUpperCase)
                        .map(metodo -> metodo + " " + caminho)
                        .forEach(operacoes::add));
        return operacoes;
    }

    private Set<String> operacoesControllers() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        Set<String> operacoes = new LinkedHashSet<>();
        for (var bean : scanner.findCandidateComponents(PACOTE_CONTROLLERS)) {
            Class<?> controller = Class.forName(bean.getBeanClassName());
            RequestMapping base = AnnotatedElementUtils.findMergedAnnotation(
                    controller,
                    RequestMapping.class);
            Set<String> bases = caminhos(base);
            for (Method method : controller.getDeclaredMethods()) {
                RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(
                        method,
                        RequestMapping.class);
                if (mapping == null) {
                    continue;
                }
                for (String basePath : bases) {
                    for (String methodPath : caminhos(mapping)) {
                        for (RequestMethod httpMethod : mapping.method()) {
                            operacoes.add(httpMethod.name() + " " + unir(basePath, methodPath));
                        }
                    }
                }
            }
        }
        return operacoes;
    }

    private Set<String> caminhos(RequestMapping mapping) {
        if (mapping == null) {
            return Set.of("");
        }
        String[] caminhos = mapping.path().length == 0 ? mapping.value() : mapping.path();
        return caminhos.length == 0
                ? Set.of("")
                : new LinkedHashSet<>(Arrays.asList(caminhos));
    }

    private String unir(String base, String complemento) {
        String caminho = ("/" + base + "/" + complemento).replaceAll("/{2,}", "/");
        return caminho.length() > 1 && caminho.endsWith("/")
                ? caminho.substring(0, caminho.length() - 1)
                : caminho;
    }

    private void validarReferencias(
            Object atual,
            Map<String, Object> raiz,
            List<String> referenciasQuebradas) {
        if (atual instanceof Map<?, ?> mapaAtual) {
            Object referencia = mapaAtual.get("$ref");
            if (referencia instanceof String ref
                    && ref.startsWith("#/")
                    && resolverReferencia(raiz, ref) == null) {
                referenciasQuebradas.add(ref);
            }
            mapaAtual.values().forEach(valor ->
                    validarReferencias(valor, raiz, referenciasQuebradas));
        } else if (atual instanceof List<?> listaAtual) {
            listaAtual.forEach(valor ->
                    validarReferencias(valor, raiz, referenciasQuebradas));
        }
    }

    private Object resolverReferencia(Map<String, Object> raiz, String referencia) {
        Object atual = raiz;
        for (String segmento : referencia.substring(2).split("/")) {
            if (!(atual instanceof Map<?, ?> mapaAtual)) {
                return null;
            }
            String chave = segmento.replace("~1", "/").replace("~0", "~");
            atual = mapaAtual.get(chave);
            if (atual == null) {
                return null;
            }
        }
        return atual;
    }

    private boolean propriedadeObrigatoria(
            Map<String, Object> schemas,
            String schema,
            String propriedade) {
        return lista(mapa(schemas.get(schema)).get("required")).contains(propriedade);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapa(Object valor) {
        return valor == null ? Map.of() : (Map<String, Object>) valor;
    }

    @SuppressWarnings("unchecked")
    private List<String> lista(Object valor) {
        return valor == null ? List.of() : (List<String>) valor;
    }
}
