package com.SistemaApiCrud.SistemaCrud.DTO;

import com.SistemaApiCrud.SistemaCrud.entity.enums.PapelUsuario;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class usuario_response_DTO {

    private Long id;

    private String username;

    private PapelUsuario role;

    private Boolean ativo;

    private Long idAluno;

    private Long idProfessor;
}
