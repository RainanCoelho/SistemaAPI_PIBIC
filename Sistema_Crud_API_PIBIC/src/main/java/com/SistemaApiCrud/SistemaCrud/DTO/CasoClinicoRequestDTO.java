package com.SistemaApiCrud.SistemaCrud.DTO;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CasoClinicoRequestDTO {

    @Size(max = 10000)
    private String sintomas;

    @Size(max = 10000)
    private String contexto;

    @Size(max = 10000)
    private String examClinico;

    @Size(max = 10000)
    private String antecClinico;

    @Size(max = 10000)
    private String diagEsperado;
}
