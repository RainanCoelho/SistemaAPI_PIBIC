package com.SistemaApiCrud.SistemaCrud.DTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RevisarRespostaRequestDTO {

    @NotNull(message = "Informe se a resposta esta correta")
    private Boolean correta;

    @NotBlank(message = "A justificativa da revisao e obrigatoria")
    @Size(max = 2000, message = "A justificativa deve ter no maximo 2000 caracteres")
    private String justificativa;
}
