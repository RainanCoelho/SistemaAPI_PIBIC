package com.SistemaApiCrud.SistemaCrud.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class conteudo_clinico_DTO {

    private Long idConteudo;

    @NotNull(message = "O caso clinico e obrigatorio")
    @Min(value = 1, message = "O caso clinico informado e invalido")
    private Long idCaso;

    @NotBlank(message = "Os sintomas sao obrigatorios")
    private String sintomas;

    @NotBlank(message = "O contexto e obrigatorio")
    private String contexto;

    @NotBlank(message = "O exame clinico e obrigatorio")
    private String examClinico;

    @NotBlank(message = "O antecedente clinico e obrigatorio")
    private String antecClinico;

    @NotBlank(message = "O diagnostico esperado e obrigatorio")
    private String diagEsperado;
}
