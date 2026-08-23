package com.SistemaApiCrud.SistemaCrud.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CasoClinicoCompletoDTO {

    private CasoClinicoResponseDTO caso;

    private List<PacienteDTO> pacientes;

    private List<ConteudoClinicoDTO> conteudosClinicos;

    private List<PerguntaResponseDTO> perguntas;
}
