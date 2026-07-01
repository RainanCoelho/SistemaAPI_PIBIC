package com.SistemaApiCrud.SistemaCrud.mapper;

import org.springframework.stereotype.Component;

import com.SistemaApiCrud.SistemaCrud.DTO.professor_request_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.professor_response_DTO;
import com.SistemaApiCrud.SistemaCrud.entity.Professor;

@Component
public class ProfessorMapper {

    public professor_response_DTO toResponse(Professor professor) {
        return new professor_response_DTO(
                professor.getId(),
                professor.getNome(),
                professor.getEmail(),
                professor.getMateria());
    }

    public Professor toEntity(professor_request_DTO dto) {
        Professor professor = new Professor();
        updateEntity(dto, professor);
        return professor;
    }

    public void updateEntity(professor_request_DTO dto, Professor professor) {
        professor.setNome(dto.getNome());
        professor.setEmail(dto.getEmail());
        professor.setMateria(dto.getMateria());
    }
}
