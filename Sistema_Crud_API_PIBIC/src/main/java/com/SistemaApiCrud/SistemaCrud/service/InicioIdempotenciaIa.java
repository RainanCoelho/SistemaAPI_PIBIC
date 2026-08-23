package com.SistemaApiCrud.SistemaCrud.service;

import com.SistemaApiCrud.SistemaCrud.entity.SolicitacaoGeracaoIa;

public record InicioIdempotenciaIa(SolicitacaoGeracaoIa solicitacao, boolean criada) {
}
