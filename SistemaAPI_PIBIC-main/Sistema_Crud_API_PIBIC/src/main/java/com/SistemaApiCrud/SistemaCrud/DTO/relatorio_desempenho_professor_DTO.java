package com.SistemaApiCrud.SistemaCrud.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class relatorio_desempenho_professor_DTO {

    private Long idProfessor;

    private Long totalRespostas;

    private Long totalCorretas;

    private Double aproveitamentoMedio;
}
