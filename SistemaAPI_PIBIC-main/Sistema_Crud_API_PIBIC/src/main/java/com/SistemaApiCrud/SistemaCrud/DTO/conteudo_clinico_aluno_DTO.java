package com.SistemaApiCrud.SistemaCrud.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class conteudo_clinico_aluno_DTO {

    private Long idConteudo;
    private Long idCaso;
    private String sintomas;
    private String contexto;
    private String examClinico;
    private String antecClinico;
}
