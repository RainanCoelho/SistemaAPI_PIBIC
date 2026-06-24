package com.SistemaApiCrud.SistemaCrud.DTO;

import jakarta.validation.constraints.Email;
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
public class aluno_request_DTO {

    @NotBlank(message = "O nome do aluno e obrigatorio")
    @Size(max = 150, message = "O nome deve ter no maximo 150 caracteres")
    private String nome;

    @NotBlank(message = "E-mail obrigatorio")
    @Email(message = "E-mail invalido")
    @Size(max = 254, message = "O e-mail deve ter no maximo 254 caracteres")
    private String email;

    @NotBlank(message = "O curso e obrigatorio")
    @Size(max = 120, message = "O curso deve ter no maximo 120 caracteres")
    private String curso;

    @NotBlank(message = "Periodo obrigatorio")
    @Size(max = 20, message = "O periodo deve ter no maximo 20 caracteres")
    private String periodo;
}
