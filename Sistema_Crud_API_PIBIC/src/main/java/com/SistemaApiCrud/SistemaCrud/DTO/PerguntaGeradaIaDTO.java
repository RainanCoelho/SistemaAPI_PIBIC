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
public class PerguntaGeradaIaDTO {

    private String texto;
    private String resposta;
    private String gabarito;
    private List<AlternativaGeradaIaDTO> alternativas;
}
