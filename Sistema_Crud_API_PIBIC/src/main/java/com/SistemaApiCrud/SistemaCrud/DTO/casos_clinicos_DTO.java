package com.SistemaApiCrud.SistemaCrud.DTO;

import java.time.LocalDateTime;

import com.SistemaApiCrud.SistemaCrud.entity.enums.NivelDificuldade;
import com.SistemaApiCrud.SistemaCrud.entity.enums.StatusCasoClinico;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
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
public class casos_clinicos_DTO {

    private Long idCaso;

    @NotNull(message = "O professor e obrigatorio")
    @Min(value = 1, message = "O professor informado e invalido")
    private Long idProfessor;

    @NotBlank(message = "O titulo do caso clinico e obrigatorio")
    @Size(max = 200, message = "O titulo deve ter no maximo 200 caracteres")
    private String titulo;

    @NotBlank(message = "A dificuldade e obrigatoria")
    @Size(max = 30, message = "A dificuldade deve ter no maximo 30 caracteres")
    private String dificuldade;

    @NotBlank(message = "A disciplina e obrigatoria")
    @Size(max = 120, message = "A disciplina deve ter no maximo 120 caracteres")
    private String disciplina;

    @NotBlank(message = "A area da saude e obrigatoria")
    @Size(max = 120, message = "A area da saude deve ter no maximo 120 caracteres")
    private String areaSaude;

    @NotBlank(message = "O estilo e obrigatorio")
    @Size(max = 60, message = "O estilo deve ter no maximo 60 caracteres")
    private String estilo;

    @NotBlank(message = "A especialidade e obrigatoria")
    @Size(max = 120, message = "A especialidade deve ter no maximo 120 caracteres")
    private String especialidade;

    private StatusCasoClinico status;

    private LocalDateTime dataCriacao;

    private LocalDateTime dataAtualizacao;

    private String objetivoAprendizagem;

    private NivelDificuldade nivelDificuldade;

    @Min(value = 15, message = "O tempo limite deve ser de pelo menos 15 minutos")
    @Max(value = 480, message = "O tempo limite deve ser de no maximo 480 minutos")
    private Integer tempoLimiteMinutos;
}
