package com.SistemaApiCrud.SistemaCrud.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class aluno_response_DTO {

    private Long idAluno;

    private String nome;

    private String email;

    private String curso;

    private String periodo;
}
