package com.SistemaApiCrud.SistemaCrud.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RelatorioDesempenhoProfessorDTO {

    private Long idProfessor;

    private Long totalRespostas;

    private Long totalAvaliadas;

    private Long totalPendentesRevisao;

    private Long totalCorretas;

    private Double aproveitamentoMedio;
}
