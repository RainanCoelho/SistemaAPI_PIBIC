package com.SistemaApiCrud.SistemaCrud.service;

import com.SistemaApiCrud.SistemaCrud.dto.CasoClinicoGeradoIaDTO;

public interface CasoClinicoAiClient {

    CasoClinicoGeradoIaDTO gerarConteudo(String instrucoesSistema, String contexto);

    default RespostaIaComMetricas<CasoClinicoGeradoIaDTO> gerarConteudoComMetricas(
            String instrucoesSistema,
            String contexto) {
        return RespostaIaComMetricas.semMetricas(gerarConteudo(instrucoesSistema, contexto));
    }
}
