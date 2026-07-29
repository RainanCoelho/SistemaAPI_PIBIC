package com.SistemaApiCrud.SistemaCrud.service;

import com.SistemaApiCrud.SistemaCrud.DTO.PerguntasGeradasIaDTO;

public interface PerguntaAiClient {

    PerguntasGeradasIaDTO gerarPerguntas(String instrucoesSistema, String contexto);
}
