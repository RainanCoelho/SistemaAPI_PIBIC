package com.SistemaApiCrud.SistemaCrud.mapper;

import org.springframework.stereotype.Component;

import com.SistemaApiCrud.SistemaCrud.dto.AlunoRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.AlunoResponseDTO;
import com.SistemaApiCrud.SistemaCrud.entity.Aluno;

@Component
public class AlunoMapper {

    public AlunoResponseDTO toResponse(Aluno aluno) {
        return new AlunoResponseDTO(
                aluno.getIdAluno(),
                aluno.getNome(),
                aluno.getEmail(),
                aluno.getCurso(),
                aluno.getPeriodo());
    }

    public Aluno toEntity(AlunoRequestDTO dto) {
        Aluno aluno = new Aluno();
        updateEntity(dto, aluno);
        return aluno;
    }

    public void updateEntity(AlunoRequestDTO dto, Aluno aluno) {
        aluno.setNome(dto.getNome());
        aluno.setEmail(dto.getEmail());
        aluno.setCurso(dto.getCurso());
        aluno.setPeriodo(dto.getPeriodo());
    }
}
