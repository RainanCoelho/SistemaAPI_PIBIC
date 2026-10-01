package com.SistemaApiCrud.SistemaCrud.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SalvarPerguntasLoteDTO(
        @NotNull @Size(min = 1, max = 100) List<@NotNull @Valid Item> perguntas) {

    public SalvarPerguntasLoteDTO {
        perguntas = perguntas == null ? null : List.copyOf(perguntas);
    }

    public record Item(@Min(1) Long id, @NotNull @Valid PerguntaRequestDTO pergunta) {
    }
}
