package com.SistemaApiCrud.SistemaCrud.mapper;

import org.springframework.stereotype.Component;

import com.SistemaApiCrud.SistemaCrud.dto.ProfessorRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.ProfessorResponseDTO;
import com.SistemaApiCrud.SistemaCrud.entity.Professor;

@Component
public class ProfessorMapper {

    public ProfessorResponseDTO toResponse(Professor professor) {
        return new ProfessorResponseDTO(
                professor.getId(),
                professor.getNome(),
                professor.getEmail(),
                professor.getMateria());
    }

    public Professor toEntity(ProfessorRequestDTO dto) {
        Professor professor = new Professor();
        updateEntity(dto, professor);
        return professor;
    }

    public void updateEntity(ProfessorRequestDTO dto, Professor professor) {
        professor.setNome(dto.getNome());
        professor.setEmail(dto.getEmail());
        professor.setMateria(dto.getMateria());
    }
}
