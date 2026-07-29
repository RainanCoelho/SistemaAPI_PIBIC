package com.SistemaApiCrud.SistemaCrud.DTO;

import jakarta.validation.constraints.NotNull;
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
}
