package com.SistemaApiCrud.SistemaCrud.DTO;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
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
    @Size(max = 100, message = "Um caso pode receber no maximo 100 respostas")
    private List<resposta_pergunta_request_DTO> respostas;
}
