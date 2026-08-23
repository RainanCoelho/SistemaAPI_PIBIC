package com.SistemaApiCrud.SistemaCrud.service;

public record RespostaIdempotente<T>(T corpo, int status) {
}
