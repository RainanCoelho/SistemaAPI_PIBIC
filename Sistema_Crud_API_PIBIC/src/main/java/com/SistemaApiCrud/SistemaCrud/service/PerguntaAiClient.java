package com.SistemaApiCrud.SistemaCrud.service;

import com.SistemaApiCrud.SistemaCrud.dto.PerguntasGeradasIaDTO;

public interface PerguntaAiClient {

    PerguntasGeradasIaDTO gerarPerguntas(String instrucoesSistema, String contexto);

    default RespostaIaComMetricas<PerguntasGeradasIaDTO> gerarPerguntasComMetricas(
            String instrucoesSistema,
            String contexto) {
        return RespostaIaComMetricas.semMetricas(gerarPerguntas(instrucoesSistema, contexto));
    }
}
