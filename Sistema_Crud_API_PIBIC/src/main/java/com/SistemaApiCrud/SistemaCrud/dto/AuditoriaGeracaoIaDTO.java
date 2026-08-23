package com.SistemaApiCrud.SistemaCrud.dto;

import java.time.Instant;

import com.SistemaApiCrud.SistemaCrud.entity.enums.OperacaoGeracaoIa;

public record AuditoriaGeracaoIaDTO(
        Long id,
        Long idCaso,
        Long idUsuario,
        OperacaoGeracaoIa operacao,
        String provedor,
        String modelo,
        String versaoPrompt,
        String hashContexto,
        String hashSaida,
        String referenciaResultado,
        Integer quantidadeItens,
        Boolean dadosDesidentificadosConfirmados,
        Instant dataGeracao) {
}
