package com.SistemaApiCrud.SistemaCrud.DTO;

import com.SistemaApiCrud.SistemaCrud.entity.enums.TipoPergunta;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GerarPerguntasIaRequestDTO {

    @NotNull(message = "A quantidade de perguntas e obrigatoria")
    @Min(value = 1, message = "A quantidade minima de perguntas e 1")
    @Max(value = 10, message = "A quantidade maxima de perguntas e 10")
    private Integer quantidade = 5;

    @NotNull(message = "O tipo da pergunta e obrigatorio")
    private TipoPergunta tipo = TipoPergunta.MULTIPLA_ESCOLHA;

    @Min(value = 2, message = "A quantidade minima de alternativas e 2")
    @Max(value = 5, message = "A quantidade maxima de alternativas e 5")
    private Integer quantidadeAlternativas = 4;

    @Size(max = 2000, message = "As instrucoes adicionais devem ter no maximo 2000 caracteres")
    private String instrucoesAdicionais;
}
