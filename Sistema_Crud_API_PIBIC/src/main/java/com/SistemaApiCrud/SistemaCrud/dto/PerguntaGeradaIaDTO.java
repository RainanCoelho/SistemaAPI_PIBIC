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
    private RubricaPerguntaDTO rubrica;
    private String gabarito;
    private List<AlternativaGeradaIaDTO> alternativas;

    public PerguntaGeradaIaDTO(
            String texto,
            String resposta,
            String gabarito,
            List<AlternativaGeradaIaDTO> alternativas) {
        this(null, texto, resposta, gabarito, alternativas, null);
    }

    public PerguntaGeradaIaDTO(
            TipoPergunta tipo,
            String texto,
            String resposta,
            String gabarito,
            List<AlternativaGeradaIaDTO> alternativas) {
        this(tipo, texto, resposta, gabarito, alternativas, null);
    }

    public PerguntaGeradaIaDTO(
            TipoPergunta tipo,
            String texto,
            String resposta,
            String gabarito,
            List<AlternativaGeradaIaDTO> alternativas,
            RubricaPerguntaDTO rubrica) {
        this.tipo = tipo;
        this.texto = texto;
        this.resposta = resposta;
        this.gabarito = gabarito;
        setAlternativas(alternativas);
        setRubrica(rubrica);
    }

    public void setAlternativas(List<AlternativaGeradaIaDTO> alternativas) {
        this.alternativas = alternativas == null
                ? null
                : alternativas.stream()
                        .map(PerguntaGeradaIaDTO::copiarAlternativa)
                        .toList();
    }

    public void setRubrica(RubricaPerguntaDTO rubrica) {
        this.rubrica = copiarRubrica(rubrica);
    }

    private static RubricaPerguntaDTO copiarRubrica(RubricaPerguntaDTO origem) {
        if (origem == null) {
            return null;
        }
        return new RubricaPerguntaDTO(
                copiarLista(origem.getCriteriosEssenciais()),
                copiarLista(origem.getCriteriosPontuacao()),
                copiarLista(origem.getErrosGraves()),
                copiarLista(origem.getJustificativas()),
                copiarLista(origem.getPrioridades()),
                copiarLista(origem.getSinaisEscalonamento()));
    }

    private static AlternativaGeradaIaDTO copiarAlternativa(AlternativaGeradaIaDTO origem) {
        return origem == null
                ? null
                : new AlternativaGeradaIaDTO(
                        origem.getLetra(),
                        origem.getTexto(),
                        origem.getCorreta());
    }

    private static <T> List<T> copiarLista(List<T> itens) {
        return itens == null ? null : List.copyOf(itens);
    }
}
