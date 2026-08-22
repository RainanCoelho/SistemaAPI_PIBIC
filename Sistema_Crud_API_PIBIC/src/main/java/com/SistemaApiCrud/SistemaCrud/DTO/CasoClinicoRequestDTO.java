package com.SistemaApiCrud.SistemaCrud.DTO;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CasoClinicoRequestDTO {

    @Size(max = 10000)
    private String sintomas;

    @Size(max = 10000)
    private String contexto;

    @Size(max = 10000)
    private String examClinico;

    @Size(max = 10000)
    private String antecClinico;

    @Size(max = 10000)
    private String diagEsperado;

    private Boolean permitirComplementoIa;

    @Size(max = 10000)
    private String informacoesAdicionaisPaciente;

    private Boolean incluirResultadosExamesClinicos;

    @AssertTrue(message = "Confirme que os dados sao sinteticos ou foram desidentificados")
    @NotNull(message = "A confirmacao sobre os dados enviados a IA e obrigatoria")
    private Boolean dadosSinteticosOuDesidentificados;

    public CasoClinicoRequestDTO(
            String sintomas,
            String contexto,
            String examClinico,
            String antecClinico,
            String diagEsperado) {
        this.sintomas = sintomas;
        this.contexto = contexto;
        this.examClinico = examClinico;
        this.antecClinico = antecClinico;
        this.diagEsperado = diagEsperado;
        this.dadosSinteticosOuDesidentificados = true;
    }
}
