package com.SistemaApiCrud.SistemaCrud.DTO;

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
public class pergunta_aluno_DTO {

    private Long id;
    private Long idCaso;
    private String texto;
    private String alternativaA;
    private String alternativaB;
    private String alternativaC;
    private String alternativaD;
    private String alternativaE;
    private List<alternativa_aluno_DTO> alternativas;
    private TipoPergunta tipo;
}
