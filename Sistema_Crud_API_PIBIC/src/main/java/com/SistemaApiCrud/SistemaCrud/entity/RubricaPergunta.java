package com.SistemaApiCrud.SistemaCrud.entity;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RubricaPergunta {

    private List<String> criteriosEssenciais;

    private List<String> criteriosPontuacao;

    private List<String> errosGraves;

    private List<String> justificativas;

    private List<String> prioridades;

    private List<String> sinaisEscalonamento;
}
