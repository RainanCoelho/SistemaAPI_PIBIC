package com.SistemaApiCrud.SistemaCrud.mapper;

import org.springframework.stereotype.Component;

import com.SistemaApiCrud.SistemaCrud.DTO.aluno_request_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.aluno_response_DTO;
import com.SistemaApiCrud.SistemaCrud.entity.Aluno;

@Component
public class AlunoMapper {

    public aluno_response_DTO toResponse(Aluno aluno) {
        return new aluno_response_DTO(
                aluno.getIdAluno(),
                aluno.getNome(),
                aluno.getEmail(),
                aluno.getCurso(),
                aluno.getPeriodo());
    }

    public Aluno toEntity(aluno_request_DTO dto) {
        Aluno aluno = new Aluno();
        updateEntity(dto, aluno);
        return aluno;
    }

    public void updateEntity(aluno_request_DTO dto, Aluno aluno) {
        aluno.setNome(dto.getNome());
        aluno.setEmail(dto.getEmail());
        aluno.setCurso(dto.getCurso());
        aluno.setPeriodo(dto.getPeriodo());
    }
}
