package com.SistemaApiCrud.SistemaCrud.DTO;

import org.springframework.data.domain.Page;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class historico_aluno_DTO {

    private Long idAluno;

    private Page<resposta_aluno_DTO> respostas;
}
