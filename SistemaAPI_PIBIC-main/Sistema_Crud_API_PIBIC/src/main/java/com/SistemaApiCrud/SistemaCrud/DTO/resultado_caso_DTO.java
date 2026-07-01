package com.SistemaApiCrud.SistemaCrud.DTO;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class resultado_caso_DTO {

    private Long idAluno;

    private Long idCaso;

    private Integer totalRespondidas;

    private Integer totalCorretas;

    private Double nota;

    private List<resposta_aluno_DTO> respostas;
}
