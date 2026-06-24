package com.SistemaApiCrud.SistemaCrud.DTO;

import java.time.LocalDateTime;

import com.SistemaApiCrud.SistemaCrud.entity.enums.NivelDificuldade;
import com.SistemaApiCrud.SistemaCrud.entity.enums.StatusCasoClinico;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class caso_clinico_response_DTO {

    private Long idCaso;

    private Long idProfessor;

    private String titulo;

    private String dificuldade;

    private String disciplina;

    private String areaSaude;

    private String estilo;

    private String especialidade;

    private StatusCasoClinico status;

    private LocalDateTime dataCriacao;

    private LocalDateTime dataAtualizacao;

    private String objetivoAprendizagem;

    private NivelDificuldade nivelDificuldade;

    private Integer tempoLimiteMinutos;
}
