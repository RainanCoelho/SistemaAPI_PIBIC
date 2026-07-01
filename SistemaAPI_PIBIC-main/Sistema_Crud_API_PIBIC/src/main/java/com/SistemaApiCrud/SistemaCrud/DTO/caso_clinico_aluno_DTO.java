package com.SistemaApiCrud.SistemaCrud.DTO;

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
public class caso_clinico_aluno_DTO {

    private caso_clinico_response_DTO caso;
    private List<paciente_DTO> pacientes;
    private List<conteudo_clinico_aluno_DTO> conteudosClinicos;
    private List<pergunta_aluno_DTO> perguntas;
    private Instant inicioTentativa;
    private Instant prazoFinal;
    private Long segundosRestantes;
}
