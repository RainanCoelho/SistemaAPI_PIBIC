package com.SistemaApiCrud.SistemaCrud.service;

import com.SistemaApiCrud.SistemaCrud.dto.CasoClinicoGeradoIaDTO;

public interface CasoClinicoAiClient {

    CasoClinicoGeradoIaDTO gerarConteudo(String instrucoesSistema, String contexto);
}
