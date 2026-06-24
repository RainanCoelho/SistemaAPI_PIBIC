package com.SistemaApiCrud.SistemaCrud.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class alternativa_pergunta_DTO {

    private Long id;

    @NotBlank(message = "A letra da alternativa e obrigatoria")
    @Size(max = 10, message = "A letra deve ter no maximo 10 caracteres")
    private String letra;

    @NotBlank(message = "O texto da alternativa e obrigatorio")
    private String texto;

    private Boolean correta;
}
