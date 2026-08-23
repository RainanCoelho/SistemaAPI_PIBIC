package com.SistemaApiCrud.SistemaCrud.dto;

import java.time.Instant;

public record RevisaoRespostaDTO(
        Long id,
        Long idResposta,
        Long idRevisor,
        Boolean correta,
        String justificativa,
        Instant dataRevisao,
        Long versaoRevisao) {
}
