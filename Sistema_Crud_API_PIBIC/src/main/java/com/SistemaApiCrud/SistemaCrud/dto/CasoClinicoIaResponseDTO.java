package com.SistemaApiCrud.SistemaCrud.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CasoClinicoIaResponseDTO {

    private Long idConteudo;
    private Long idCaso;
    private String sintomas;
    private String contexto;
    private String examClinico;
    private String antecClinico;
    private String diagEsperado;
    private CasoClinicoCompletoDTO completo;
}
