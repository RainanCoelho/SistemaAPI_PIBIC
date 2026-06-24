package com.SistemaApiCrud.SistemaCrud.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.SistemaApiCrud.SistemaCrud.DTO.alternativa_pergunta_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.pergunta_request_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.pergunta_response_DTO;
import com.SistemaApiCrud.SistemaCrud.entity.casos_clinicos;
import com.SistemaApiCrud.SistemaCrud.entity.pergunta;

@Component
public class PerguntaMapper {

    public pergunta_response_DTO toResponse(pergunta pergunta, List<alternativa_pergunta_DTO> alternativas) {
        Long idCaso = pergunta.getCasoClinico() != null ? pergunta.getCasoClinico().getIdCaso() : null;

        return new pergunta_response_DTO(
                pergunta.getId(),
                idCaso,
                pergunta.getTexto(),
                pergunta.getAlternativaA(),
                pergunta.getAlternativaB(),
                pergunta.getAlternativaC(),
                pergunta.getAlternativaD(),
                pergunta.getAlternativaE(),
                alternativas,
                pergunta.getResposta(),
                pergunta.getTipo(),
                pergunta.getGabarito());
    }

    public pergunta toEntity(pergunta_request_DTO dto, casos_clinicos caso) {
        pergunta pergunta = new pergunta();
        updateEntity(dto, pergunta, caso);
        return pergunta;
    }

    public void updateEntity(pergunta_request_DTO dto, pergunta pergunta, casos_clinicos caso) {
        if (caso != null) {
            pergunta.setCasoClinico(caso);
        }

        pergunta.setTexto(dto.getTexto());
        pergunta.setAlternativaA(dto.getAlternativaA());
        pergunta.setAlternativaB(dto.getAlternativaB());
        pergunta.setAlternativaC(dto.getAlternativaC());
        pergunta.setAlternativaD(dto.getAlternativaD());
        pergunta.setAlternativaE(dto.getAlternativaE());
        pergunta.setResposta(dto.getResposta());
        pergunta.setTipo(dto.getTipo());
        pergunta.setGabarito(dto.getGabarito());
    }
}
