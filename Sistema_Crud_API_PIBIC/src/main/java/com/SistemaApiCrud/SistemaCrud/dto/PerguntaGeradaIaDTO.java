package com.SistemaApiCrud.SistemaCrud.dto;

import java.util.List;

import com.SistemaApiCrud.SistemaCrud.entity.enums.TipoPergunta;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PerguntaGeradaIaDTO {

    private TipoPergunta tipo;

    private String texto;
    private String resposta;
    private String gabarito;
    private List<AlternativaGeradaIaDTO> alternativas;

    public PerguntaGeradaIaDTO(
            String texto,
            String resposta,
            String gabarito,
            List<AlternativaGeradaIaDTO> alternativas) {
        this(null, texto, resposta, gabarito, alternativas);
    }

    public PerguntaGeradaIaDTO(
            TipoPergunta tipo,
            String texto,
            String resposta,
            String gabarito,
            List<AlternativaGeradaIaDTO> alternativas) {
        this.tipo = tipo;
        this.texto = texto;
        this.resposta = resposta;
        this.gabarito = gabarito;
        this.alternativas = alternativas;
    }
}
