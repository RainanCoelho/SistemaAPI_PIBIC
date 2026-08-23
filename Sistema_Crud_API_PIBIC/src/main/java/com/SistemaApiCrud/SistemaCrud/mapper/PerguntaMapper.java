package com.SistemaApiCrud.SistemaCrud.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.SistemaApiCrud.SistemaCrud.dto.AlternativaPerguntaDTO;
import com.SistemaApiCrud.SistemaCrud.dto.PerguntaRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.PerguntaResponseDTO;
import com.SistemaApiCrud.SistemaCrud.entity.CasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.Pergunta;

@Component
public class PerguntaMapper {

    public PerguntaResponseDTO toResponse(Pergunta pergunta, List<AlternativaPerguntaDTO> alternativas) {
        Long idCaso = pergunta.getCasoClinico() != null ? pergunta.getCasoClinico().getIdCaso() : null;

        return new PerguntaResponseDTO(
                pergunta.getId(),
                idCaso,
                pergunta.getTexto(),
                alternativas,
                pergunta.getResposta(),
                pergunta.getTipo(),
                pergunta.getGabarito());
    }

    public Pergunta toEntity(PerguntaRequestDTO dto, CasoClinico caso) {
        Pergunta pergunta = new Pergunta();
        updateEntity(dto, pergunta, caso);
        return pergunta;
    }

    public void updateEntity(PerguntaRequestDTO dto, Pergunta pergunta, CasoClinico caso) {
        if (caso != null) {
            pergunta.setCasoClinico(caso);
        }

        pergunta.setTexto(dto.getTexto());
        pergunta.setResposta(dto.getResposta());
        pergunta.setTipo(dto.getTipo());
        pergunta.setGabarito(dto.getGabarito());
    }
}
