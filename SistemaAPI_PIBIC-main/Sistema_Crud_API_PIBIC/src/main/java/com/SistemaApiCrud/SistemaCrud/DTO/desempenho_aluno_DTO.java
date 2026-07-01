package com.SistemaApiCrud.SistemaCrud.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class desempenho_aluno_DTO {

    private Long idAluno;

    private Long totalRespostas;

    private Long totalCorretas;

    private Double aproveitamento;
}
