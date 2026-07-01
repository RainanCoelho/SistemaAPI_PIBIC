package com.SistemaApiCrud.SistemaCrud.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class professor_response_DTO {

    private Long id;

    private String nome;

    private String email;

    private String materia;
}
