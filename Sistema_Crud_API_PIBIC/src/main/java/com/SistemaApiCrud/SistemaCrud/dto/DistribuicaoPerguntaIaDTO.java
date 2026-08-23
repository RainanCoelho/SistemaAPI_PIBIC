package com.SistemaApiCrud.SistemaCrud.dto;

import com.SistemaApiCrud.SistemaCrud.entity.enums.TipoPergunta;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DistribuicaoPerguntaIaDTO {

    @NotNull(message = "O tipo da pergunta e obrigatorio na distribuicao")
    private TipoPergunta tipo;

    @NotNull(message = "A quantidade e obrigatoria na distribuicao")
    @Min(value = 1, message = "A quantidade minima por tipo e 1")
    @Max(value = 10, message = "A quantidade maxima por tipo e 10")
    private Integer quantidade;

    @Min(value = 2, message = "A quantidade minima de alternativas e 2")
    @Max(value = 5, message = "A quantidade maxima de alternativas e 5")
    private Integer quantidadeAlternativas;
}
