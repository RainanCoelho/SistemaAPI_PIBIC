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
public class resposta_pergunta_request_DTO {

    @NotNull(message = "A pergunta e obrigatoria")
    @Min(value = 1, message = "A pergunta informada e invalida")
    private Long idPergunta;

    @NotBlank(message = "A resposta marcada e obrigatoria")
    private String respostaMarcada;
}
