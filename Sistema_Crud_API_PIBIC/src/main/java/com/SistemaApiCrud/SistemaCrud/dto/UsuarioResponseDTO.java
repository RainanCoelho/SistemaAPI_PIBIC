package com.SistemaApiCrud.SistemaCrud.dto;

import com.SistemaApiCrud.SistemaCrud.entity.enums.PapelUsuario;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponseDTO {

    private Long id;

    private String username;

    private PapelUsuario role;

    private Boolean ativo;

    private Long idAluno;

    private Long idProfessor;
}
