package com.SistemaApiCrud.SistemaCrud.service;

import com.SistemaApiCrud.SistemaCrud.dto.PerguntasGeradasIaDTO;

public interface PerguntaAiClient {

    PerguntasGeradasIaDTO gerarPerguntas(String instrucoesSistema, String contexto);
}
