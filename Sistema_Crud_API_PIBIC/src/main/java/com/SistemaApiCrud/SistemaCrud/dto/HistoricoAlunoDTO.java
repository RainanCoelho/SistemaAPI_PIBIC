package com.SistemaApiCrud.SistemaCrud.dto;

import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HistoricoAlunoDTO {

    private Long idAluno;

    private Page<RespostaAlunoDTO> respostas;
}
