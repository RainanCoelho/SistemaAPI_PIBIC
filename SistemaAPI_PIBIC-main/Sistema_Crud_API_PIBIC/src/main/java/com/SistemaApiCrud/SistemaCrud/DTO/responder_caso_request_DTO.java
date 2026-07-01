package com.SistemaApiCrud.SistemaCrud.DTO;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class responder_caso_request_DTO {

    @Valid
    @NotEmpty(message = "Informe pelo menos uma resposta")
    private List<resposta_pergunta_request_DTO> respostas;
}
