package com.SistemaApiCrud.SistemaCrud.mapper;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.SistemaApiCrud.SistemaCrud.DTO.LoginResponseDTO;
import com.SistemaApiCrud.SistemaCrud.DTO.usuario_response_DTO;
import com.SistemaApiCrud.SistemaCrud.entity.Usuario;

@Component
public class UsuarioMapper {

    public usuario_response_DTO toResponse(Usuario usuario) {
        return new usuario_response_DTO(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getRole(),
                usuario.getAtivo(),
                getIdAluno(usuario),
                getIdProfessor(usuario));
    }

    public LoginResponseDTO toLoginResponse(String token, Instant expiraEm, Usuario usuario) {
        return new LoginResponseDTO(
                token,
                "Bearer",
                expiraEm,
                usuario.getId(),
                usuario.getUsername(),
                usuario.getRole(),
                getIdAluno(usuario),
                getIdProfessor(usuario));
    }

    private Long getIdAluno(Usuario usuario) {
        return usuario.getAluno() != null ? usuario.getAluno().getIdAluno() : null;
    }

    private Long getIdProfessor(Usuario usuario) {
        return usuario.getProfessor() != null ? usuario.getProfessor().getId() : null;
    }
}
