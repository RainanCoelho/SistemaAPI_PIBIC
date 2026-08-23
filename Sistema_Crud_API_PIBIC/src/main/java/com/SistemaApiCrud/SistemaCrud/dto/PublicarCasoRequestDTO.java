package com.SistemaApiCrud.SistemaCrud.dto;

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
public class PublicarCasoRequestDTO {

    @NotNull(message = "O tempo limite e obrigatorio quando a configuracao de publicacao e informada")
    @Min(value = 15, message = "O tempo limite deve ser de pelo menos 15 minutos")
    @Max(value = 480, message = "O tempo limite deve ser de no maximo 480 minutos")
    private Integer tempoLimiteMinutos;
}
