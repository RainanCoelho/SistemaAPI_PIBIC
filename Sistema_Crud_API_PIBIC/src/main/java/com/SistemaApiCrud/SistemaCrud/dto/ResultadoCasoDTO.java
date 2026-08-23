package com.SistemaApiCrud.SistemaCrud.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoCasoDTO {

    private Long idAluno;

    private Long idCaso;

    private Integer totalRespondidas;

    private Integer totalAvaliadas;

    private Integer totalPendentesRevisao;

    private Integer totalCorretas;

    private Double nota;

    private List<RespostaAlunoDTO> respostas;
}
