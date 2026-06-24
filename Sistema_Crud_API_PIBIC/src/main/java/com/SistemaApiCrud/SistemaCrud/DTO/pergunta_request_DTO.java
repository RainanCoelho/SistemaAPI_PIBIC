package com.SistemaApiCrud.SistemaCrud.DTO;

import java.util.List;

import com.SistemaApiCrud.SistemaCrud.entity.enums.TipoPergunta;

import jakarta.validation.Valid;
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
public class pergunta_request_DTO {

    @Min(value = 1, message = "O caso clinico informado e invalido")
    private Long idCaso;

    @NotBlank(message = "Pergunta vazia")
    private String texto;

    private String alternativaA;

    private String alternativaB;

    private String alternativaC;

    private String alternativaD;

    private String alternativaE;

    @Valid
    private List<alternativa_pergunta_DTO> alternativas;

    @NotBlank(message = "A resposta e obrigatoria")
    private String resposta;

    @NotNull(message = "O tipo da pergunta e obrigatorio")
    private TipoPergunta tipo;

    @NotBlank(message = "O gabarito e obrigatorio")
    private String gabarito;

}
