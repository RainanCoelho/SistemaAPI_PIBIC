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
public class CasoClinicoAjusteRequestDTO {

    @NotBlank(message = "O tipo de ajuste e obrigatorio")
    @Size(max = 40)
    private String tipoAjuste;

    @Size(max = 2000)
    private String instrucao;
}
