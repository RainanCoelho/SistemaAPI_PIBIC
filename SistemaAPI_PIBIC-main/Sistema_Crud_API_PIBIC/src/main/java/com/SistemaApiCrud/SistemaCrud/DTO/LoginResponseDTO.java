package com.SistemaApiCrud.SistemaCrud.DTO;

import java.time.Instant;

import com.SistemaApiCrud.SistemaCrud.entity.enums.PapelUsuario;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {

    private String token;

    private String tipo;

    private Instant expiraEm;

    private Long idUsuario;

    private String username;

    private PapelUsuario role;

    private Long idAluno;

    private Long idProfessor;
}
