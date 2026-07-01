package com.SistemaApiCrud.SistemaCrud.DTO;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class resposta_aluno_DTO {

    private Long id;

    private Long idAluno;

    private Long idCaso;

    private Long idPergunta;

    private String respostaMarcada;

    private Boolean correta;

    private LocalDateTime dataResposta;
}
