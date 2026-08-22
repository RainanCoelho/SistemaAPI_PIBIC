package com.SistemaApiCrud.SistemaCrud.DTO;

import jakarta.validation.constraints.NotBlank;
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
public class CasoClinicoAjusteRequestDTO {

    @NotBlank(message = "O tipo de ajuste e obrigatorio")
    @Size(max = 40)
    private String tipoAjuste;

    @Size(max = 2000)
    private String instrucao;

    @AssertTrue(message = "Confirme que os dados sao sinteticos ou foram desidentificados")
    @NotNull(message = "A confirmacao sobre os dados enviados a IA e obrigatoria")
    private Boolean dadosSinteticosOuDesidentificados;
}
