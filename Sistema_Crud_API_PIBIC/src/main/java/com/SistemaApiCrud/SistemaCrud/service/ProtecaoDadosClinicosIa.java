package com.SistemaApiCrud.SistemaCrud.service;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class ProtecaoDadosClinicosIa {

    private static final String VALOR_REDACTADO = "[DADO_REMOVIDO]";
    private static final Pattern EMAIL = Pattern.compile(
            "(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");
    private static final Pattern CPF = Pattern.compile(
            "(?<!\\d)\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}(?!\\d)");
    private static final Pattern CNS = Pattern.compile(
            "(?<!\\d)\\d{15}(?!\\d)");
    private static final Pattern TELEFONE = Pattern.compile(
            "(?<!\\d)(?:\\+?55\\s*)?(?:\\(?\\d{2}\\)?\\s*)?9?\\d{4}[-\\s]?\\d{4}(?!\\d)");
    private static final Pattern DATA_EXATA = Pattern.compile(
            "(?<!\\d)(?:\\d{2}[/-]\\d{2}[/-]\\d{4}|\\d{4}-\\d{2}-\\d{2})(?!\\d)");
    private static final Pattern IDENTIFICADOR_ROTULADO = Pattern.compile(
            "(?i)\\b(?:nome(?:\\s+completo)?|cpf|rg|prontu[aá]rio|endere[cç]o|"
                    + "e-?mail|telefone|celular)\\s*[:=]\\s*[^;\\n]{1,150}");

    public String prepararParaEnvio(String valor) {
        if (valor == null) {
            return null;
        }

        String protegido = valor.trim().replace("\u0000", "");
        protegido = IDENTIFICADOR_ROTULADO.matcher(protegido).replaceAll(VALOR_REDACTADO);
        protegido = EMAIL.matcher(protegido).replaceAll(VALOR_REDACTADO);
        protegido = CPF.matcher(protegido).replaceAll(VALOR_REDACTADO);
        protegido = CNS.matcher(protegido).replaceAll(VALOR_REDACTADO);
        protegido = TELEFONE.matcher(protegido).replaceAll(VALOR_REDACTADO);
        protegido = DATA_EXATA.matcher(protegido).replaceAll(VALOR_REDACTADO);
        return protegido
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
