package com.SistemaApiCrud.SistemaCrud.DTO;

import java.util.List;

import com.SistemaApiCrud.SistemaCrud.entity.enums.TipoPergunta;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class pergunta_request_DTO {

    @Min(value = 1, message = "O caso clinico informado e invalido")
    private Long idCaso;

    @NotBlank(message = "Pergunta vazia")
    @Size(max = 10000, message = "A pergunta deve ter no maximo 10000 caracteres")
    private String texto;

    @Size(max = 5, message = "A pergunta deve ter no maximo 5 alternativas")
    private List<@Valid alternativa_pergunta_DTO> alternativas;

    @NotBlank(message = "A resposta e obrigatoria")
    @Size(max = 10000, message = "A resposta deve ter no maximo 10000 caracteres")
    private String resposta;

    @NotNull(message = "O tipo da pergunta e obrigatorio")
    private TipoPergunta tipo;

    @NotBlank(message = "O gabarito e obrigatorio")
    @Size(max = 10000, message = "O gabarito deve ter no maximo 10000 caracteres")
    private String gabarito;

}
