package com.SistemaApiCrud.SistemaCrud.dto;

import java.util.List;

import com.SistemaApiCrud.SistemaCrud.entity.enums.TipoPergunta;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PerguntaAlunoDTO {

    private Long id;
    private Long idCaso;
    private String texto;
    private List<AlternativaAlunoDTO> alternativas;
    private TipoPergunta tipo;
}
