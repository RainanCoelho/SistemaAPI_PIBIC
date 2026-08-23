package com.SistemaApiCrud.SistemaCrud.mapper;

import org.springframework.stereotype.Component;

import com.SistemaApiCrud.SistemaCrud.dto.CasoClinicoRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.CasoClinicoResponseDTO;
import com.SistemaApiCrud.SistemaCrud.entity.Professor;
import com.SistemaApiCrud.SistemaCrud.entity.CasoClinico;

@Component
public class CasoClinicoMapper {

    public CasoClinicoResponseDTO toResponse(CasoClinico caso) {
        Long idProfessor = caso.getProfessor() != null ? caso.getProfessor().getId() : null;

        return new CasoClinicoResponseDTO(
                caso.getIdCaso(),
                idProfessor,
                caso.getTitulo(),
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

    public CasoClinico toEntity(CasoClinicoRequestDTO dto, Professor professor) {
        CasoClinico caso = new CasoClinico();
        updateEntity(dto, caso, professor);
        return caso;
    }

    public void updateEntity(CasoClinicoRequestDTO dto, CasoClinico caso, Professor professor) {
        if (professor != null) {
            caso.setProfessor(professor);
        }

        caso.setTitulo(dto.getTitulo());
        caso.setDisciplina(dto.getDisciplina());
        caso.setAreaSaude(dto.getAreaSaude());
        caso.setEstilo(dto.getEstilo());
        caso.setEspecialidade(dto.getEspecialidade());
        caso.setObjetivoAprendizagem(dto.getObjetivoAprendizagem());
        caso.setNivelDificuldade(dto.getNivelDificuldade());
        if (dto.getTempoLimiteMinutos() != null) {
            caso.setTempoLimiteMinutos(dto.getTempoLimiteMinutos());
        } else if (caso.getTempoLimiteMinutos() == null) {
            caso.setTempoLimiteMinutos(CasoClinico.TEMPO_LIMITE_PADRAO_MINUTOS);
        }
    }
}
