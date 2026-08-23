package com.SistemaApiCrud.SistemaCrud.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfessorResponseDTO {

    private Long id;

    private String nome;

    private String email;

    private String materia;
}
