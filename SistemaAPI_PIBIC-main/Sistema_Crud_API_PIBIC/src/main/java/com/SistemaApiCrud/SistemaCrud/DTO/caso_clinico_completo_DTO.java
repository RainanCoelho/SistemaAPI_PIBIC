package com.SistemaApiCrud.SistemaCrud.DTO;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class caso_clinico_completo_DTO {

    private caso_clinico_response_DTO caso;

    private List<paciente_DTO> pacientes;

    private List<conteudo_clinico_DTO> conteudosClinicos;

    private List<pergunta_response_DTO> perguntas;
}
