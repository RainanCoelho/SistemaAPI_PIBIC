package com.SistemaApiCrud.SistemaCrud.service;

import com.SistemaApiCrud.SistemaCrud.DTO.CasoClinicoGeradoIaDTO;

public interface CasoClinicoAiClient {

    CasoClinicoGeradoIaDTO gerarConteudo(String instrucoesSistema, String contexto);
}
