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
public class professor_cadastro_request_DTO {

    @NotBlank(message = "O nome do professor e obrigatorio")
    @Size(max = 150, message = "O nome deve ter no maximo 150 caracteres")
    private String nome;

    @NotBlank(message = "E-mail obrigatorio")
    @Email(message = "E-mail invalido")
    @Size(max = 254, message = "O e-mail deve ter no maximo 254 caracteres")
    private String email;

    @NotBlank(message = "A materia e obrigatoria")
    @Size(max = 120, message = "A materia deve ter no maximo 120 caracteres")
    private String materia;

    @NotBlank(message = "O usuario e obrigatorio")
    @Size(min = 3, max = 100, message = "O usuario deve ter entre 3 e 100 caracteres")
    private String username;

    @NotBlank(message = "A senha e obrigatoria")
    @Size(min = 12, max = 72, message = "A senha deve ter entre 12 e 72 caracteres")
    private String senha;
}
