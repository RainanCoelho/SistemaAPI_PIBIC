package com.SistemaApiCrud.SistemaCrud.service;

import java.util.List;

import com.SistemaApiCrud.SistemaCrud.dto.RubricaPerguntaDTO;
import com.SistemaApiCrud.SistemaCrud.entity.enums.TipoPergunta;

public final class RubricaPerguntaValidator {

    private static final int MAXIMO_ITENS_POR_SECAO = 10;
    private static final int MAXIMO_CARACTERES_POR_ITEM = 2_000;
    private static final int MAXIMO_CARACTERES_TOTAL = 10_000;

    private RubricaPerguntaValidator() {
    }

    public static String encontrarErro(RubricaPerguntaDTO rubrica, TipoPergunta tipo) {
        if (rubrica == null) {
            return null;
        }
        if (tipo != TipoPergunta.DISCURSIVA && tipo != TipoPergunta.CONDUTA_CLINICA) {
            return "Somente perguntas discursivas e de conduta aceitam rubrica estruturada";
        }

        List<SecaoRubrica> secoes = List.of(
                new SecaoRubrica("criterios essenciais", rubrica.getCriteriosEssenciais()),
                new SecaoRubrica("criterios de pontuacao", rubrica.getCriteriosPontuacao()),
                new SecaoRubrica("erros graves", rubrica.getErrosGraves()),
                new SecaoRubrica("justificativas", rubrica.getJustificativas()),
                new SecaoRubrica("prioridades", rubrica.getPrioridades()),
                new SecaoRubrica("sinais de escalonamento", rubrica.getSinaisEscalonamento()));

        int totalCaracteres = 0;
        for (SecaoRubrica secao : secoes) {
            String erro = validarSecao(secao);
            if (erro != null) {
                return erro;
            }
            totalCaracteres += tamanhoTotal(secao.itens());
        }
        if (totalCaracteres > MAXIMO_CARACTERES_TOTAL) {
            return "A rubrica estruturada deve ter no maximo 10000 caracteres";
        }
        if (!possuiItens(rubrica.getCriteriosEssenciais())) {
            return "A rubrica estruturada deve informar criterios essenciais";
        }
        if (tipo == TipoPergunta.CONDUTA_CLINICA && !possuiItens(rubrica.getPrioridades())) {
            return "A rubrica de conduta clinica deve informar prioridades";
        }
        if (tipo == TipoPergunta.DISCURSIVA
                && (possuiItens(rubrica.getPrioridades())
                        || possuiItens(rubrica.getSinaisEscalonamento()))) {
            return "Prioridades e sinais de escalonamento sao exclusivos da conduta clinica";
        }
        return null;
    }

    private static String validarSecao(SecaoRubrica secao) {
        if (secao.itens() == null) {
            return null;
        }
        if (secao.itens().size() > MAXIMO_ITENS_POR_SECAO) {
            return "A secao de " + secao.nome() + " deve ter no maximo 10 itens";
        }
        for (String item : secao.itens()) {
            if (item == null || item.isBlank()) {
                return "A secao de " + secao.nome() + " nao pode conter itens vazios";
            }
            if (item.length() > MAXIMO_CARACTERES_POR_ITEM) {
                return "Cada item de " + secao.nome() + " deve ter no maximo 2000 caracteres";
            }
        }
        return null;
    }

    private static int tamanhoTotal(List<String> itens) {
        return itens == null ? 0 : itens.stream().mapToInt(String::length).sum();
    }

    private static boolean possuiItens(List<String> itens) {
        return itens != null && !itens.isEmpty();
    }

    private record SecaoRubrica(String nome, List<String> itens) {
    }
}
