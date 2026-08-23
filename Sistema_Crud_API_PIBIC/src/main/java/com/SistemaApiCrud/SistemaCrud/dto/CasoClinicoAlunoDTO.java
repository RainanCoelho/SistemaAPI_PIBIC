package com.SistemaApiCrud.SistemaCrud.dto;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CasoClinicoAlunoDTO {

    private CasoClinicoResponseDTO caso;
    private List<PacienteDTO> pacientes;
    private List<ConteudoClinicoAlunoDTO> conteudosClinicos;
    private List<PerguntaAlunoDTO> perguntas;
    private Instant inicioTentativa;
    private Instant prazoFinal;
    private Long segundosRestantes;
}
