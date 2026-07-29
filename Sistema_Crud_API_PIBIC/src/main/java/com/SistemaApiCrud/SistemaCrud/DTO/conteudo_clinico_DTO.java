package com.SistemaApiCrud.SistemaCrud.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @Size(max = 10000, message = "Os sintomas devem ter no maximo 10000 caracteres")
    private String sintomas;

    @NotBlank(message = "O contexto e obrigatorio")
    @Size(max = 10000, message = "O contexto deve ter no maximo 10000 caracteres")
    private String contexto;

    @NotBlank(message = "O exame clinico e obrigatorio")
    @Size(max = 10000, message = "O exame clinico deve ter no maximo 10000 caracteres")
    private String examClinico;

    @NotBlank(message = "O antecedente clinico e obrigatorio")
    @Size(max = 10000, message = "O antecedente clinico deve ter no maximo 10000 caracteres")
    private String antecClinico;

    @NotBlank(message = "O diagnostico esperado e obrigatorio")
    @Size(max = 10000, message = "O diagnostico esperado deve ter no maximo 10000 caracteres")
    private String diagEsperado;
}
