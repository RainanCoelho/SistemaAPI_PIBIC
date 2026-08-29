package com.SistemaApiCrud.SistemaCrud.mapper;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.SistemaApiCrud.SistemaCrud.dto.AlternativaPerguntaDTO;
import com.SistemaApiCrud.SistemaCrud.dto.PerguntaRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.PerguntaResponseDTO;
import com.SistemaApiCrud.SistemaCrud.dto.RubricaPerguntaDTO;
import com.SistemaApiCrud.SistemaCrud.entity.CasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.Pergunta;
import com.SistemaApiCrud.SistemaCrud.entity.RubricaPergunta;

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
                toRubricaResponse(pergunta.getRubrica()),
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
        pergunta.setRubrica(toRubricaEntity(dto.getRubrica()));
        pergunta.setTipo(dto.getTipo());
        pergunta.setGabarito(dto.getGabarito());
    }

    private RubricaPergunta toRubricaEntity(RubricaPerguntaDTO rubrica) {
        if (rubrica == null) {
            return null;
        }
        return new RubricaPergunta(
                copyItems(rubrica.getCriteriosEssenciais()),
                copyItems(rubrica.getCriteriosPontuacao()),
                copyItems(rubrica.getErrosGraves()),
                copyItems(rubrica.getJustificativas()),
                copyItems(rubrica.getPrioridades()),
                copyItems(rubrica.getSinaisEscalonamento()));
    }

    private RubricaPerguntaDTO toRubricaResponse(RubricaPergunta rubrica) {
        if (rubrica == null) {
            return null;
        }
        return new RubricaPerguntaDTO(
                copyItems(rubrica.getCriteriosEssenciais()),
                copyItems(rubrica.getCriteriosPontuacao()),
                copyItems(rubrica.getErrosGraves()),
                copyItems(rubrica.getJustificativas()),
                copyItems(rubrica.getPrioridades()),
                copyItems(rubrica.getSinaisEscalonamento()));
    }

    private List<String> copyItems(List<String> items) {
        if (items == null) {
            return null;
        }
        return items.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }
}
