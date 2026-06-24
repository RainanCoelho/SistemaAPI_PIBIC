package com.SistemaApiCrud.SistemaCrud.DTO;

import com.SistemaApiCrud.SistemaCrud.entity.enums.PapelUsuario;

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
public class usuario_request_DTO {

    @NotBlank(message = "O usuario e obrigatorio")
    @Size(min = 3, max = 100, message = "O usuario deve ter entre 3 e 100 caracteres")
    private String username;

    @NotBlank(message = "A senha e obrigatoria")
    @Size(min = 12, max = 72, message = "A senha deve ter entre 12 e 72 caracteres")
    private String senha;

    @NotNull(message = "A role e obrigatoria")
    private PapelUsuario role;

    private Boolean ativo;

    @Min(value = 1, message = "O aluno informado e invalido")
    private Long idAluno;

    @Min(value = 1, message = "O professor informado e invalido")
    private Long idProfessor;
}
