package com.SistemaApiCrud.SistemaCrud.mapper;

import org.springframework.stereotype.Component;

import com.SistemaApiCrud.SistemaCrud.DTO.caso_clinico_request_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.caso_clinico_response_DTO;
import com.SistemaApiCrud.SistemaCrud.entity.Professor;
import com.SistemaApiCrud.SistemaCrud.entity.casos_clinicos;

@Component
public class CasoClinicoMapper {

    public caso_clinico_response_DTO toResponse(casos_clinicos caso) {
        Long idProfessor = caso.getProfessor() != null ? caso.getProfessor().getId() : null;

        return new caso_clinico_response_DTO(
                caso.getIdCaso(),
                idProfessor,
                caso.getTitulo(),
                caso.getDificuldade(),
                caso.getDisciplina(),
                caso.getAreaSaude(),
                caso.getEstilo(),
                caso.getEspecialidade(),
                caso.getStatus(),
                caso.getDataCriacao(),
                caso.getDataAtualizacao(),
                caso.getObjetivoAprendizagem(),
                caso.getNivelDificuldade(),
                caso.getTempoLimiteMinutos());
    }

    public casos_clinicos toEntity(caso_clinico_request_DTO dto, Professor professor) {
        casos_clinicos caso = new casos_clinicos();
        updateEntity(dto, caso, professor);
        return caso;
    }

    public void updateEntity(caso_clinico_request_DTO dto, casos_clinicos caso, Professor professor) {
        if (professor != null) {
            caso.setProfessor(professor);
        }

        caso.setTitulo(dto.getTitulo());
        caso.setDificuldade(dto.getDificuldade());
        caso.setDisciplina(dto.getDisciplina());
        caso.setAreaSaude(dto.getAreaSaude());
        caso.setEstilo(dto.getEstilo());
        caso.setEspecialidade(dto.getEspecialidade());
        caso.setStatus(dto.getStatus());
        caso.setObjetivoAprendizagem(dto.getObjetivoAprendizagem());
        caso.setNivelDificuldade(dto.getNivelDificuldade());
        if (dto.getTempoLimiteMinutos() != null) {
            caso.setTempoLimiteMinutos(dto.getTempoLimiteMinutos());
        } else if (caso.getTempoLimiteMinutos() == null) {
            caso.setTempoLimiteMinutos(casos_clinicos.TEMPO_LIMITE_PADRAO_MINUTOS);
        }
    }
}
