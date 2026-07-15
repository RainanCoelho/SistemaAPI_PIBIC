package com.SistemaApiCrud.SistemaCrud.DTO;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "nome",
        "idade",
        "sexo",
        "estadoCivil",
        "profissao",
        "peso",
        "altura"
})
public class PacienteGeradoIaDTO {

    private String nome;
    private Integer idade;
    private String sexo;
    private String estadoCivil;
    private String profissao;
    private String peso;
    private String altura;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Integer getIdade() {
        return idade;
    }

    public void setIdade(Integer idade) {
        this.idade = idade;
    }

    public String getSexo() {
        return sexo;
    }

    public void setSexo(String sexo) {
        this.sexo = sexo;
    }

    public String getEstadoCivil() {
        return estadoCivil;
    }

    public void setEstadoCivil(String estadoCivil) {
        this.estadoCivil = estadoCivil;
    }

    public String getProfissao() {
        return profissao;
    }

    public void setProfissao(String profissao) {
        this.profissao = profissao;
    }

    public String getPeso() {
        return peso;
    }

    public void setPeso(String peso) {
        this.peso = peso;
    }

    public String getAltura() {
        return altura;
    }

    public void setAltura(String altura) {
        this.altura = altura;
    }
}
