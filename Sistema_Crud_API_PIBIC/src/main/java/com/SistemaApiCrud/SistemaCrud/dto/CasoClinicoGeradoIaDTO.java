package com.SistemaApiCrud.SistemaCrud.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "sintomas",
        "contexto",
        "examClinico",
        "antecClinico",
        "diagEsperado",
        "objetivoAprendizagem",
        "paciente"
})
public class CasoClinicoGeradoIaDTO {

    private String sintomas;
    private String contexto;
    private String examClinico;
    private String antecClinico;
    private String diagEsperado;
    private String objetivoAprendizagem;
    private PacienteGeradoIaDTO paciente;

    public String getSintomas() {
        return sintomas;
    }

    public void setSintomas(String sintomas) {
        this.sintomas = sintomas;
    }

    public String getContexto() {
        return contexto;
    }

    public void setContexto(String contexto) {
        this.contexto = contexto;
    }

    public String getExamClinico() {
        return examClinico;
    }

    public void setExamClinico(String examClinico) {
        this.examClinico = examClinico;
    }

    public String getAntecClinico() {
        return antecClinico;
    }

    public void setAntecClinico(String antecClinico) {
        this.antecClinico = antecClinico;
    }

    public String getDiagEsperado() {
        return diagEsperado;
    }

    public void setDiagEsperado(String diagEsperado) {
        this.diagEsperado = diagEsperado;
    }

    public String getObjetivoAprendizagem() {
        return objetivoAprendizagem;
    }

    public void setObjetivoAprendizagem(String objetivoAprendizagem) {
        this.objetivoAprendizagem = objetivoAprendizagem;
    }

    public PacienteGeradoIaDTO getPaciente() {
        return paciente;
    }

    public void setPaciente(PacienteGeradoIaDTO paciente) {
        this.paciente = paciente;
    }
}
