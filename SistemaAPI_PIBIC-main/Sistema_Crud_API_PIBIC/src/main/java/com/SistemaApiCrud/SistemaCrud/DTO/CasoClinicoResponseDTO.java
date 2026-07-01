package com.SistemaApiCrud.SistemaCrud.DTO;


import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CasoClinicoResponseDTO {
    //Resultado Devolvido
    private String sintomas;
    private String contexto;
    private String examClinico;
    private String antecClinico;
    private String diagEsperado;
}
