package com.SistemaApiCrud.SistemaCrud.dto;

import java.util.List;

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
public class RubricaPerguntaDTO {

    @Size(max = 10, message = "A rubrica deve ter no maximo 10 criterios essenciais")
    private List<
            @NotBlank(message = "Os criterios essenciais nao podem conter itens vazios")
            @Size(max = 2000, message = "Cada criterio essencial deve ter no maximo 2000 caracteres")
            String> criteriosEssenciais;

    @Size(max = 10, message = "A rubrica deve ter no maximo 10 criterios de pontuacao")
    private List<
            @NotBlank(message = "Os criterios de pontuacao nao podem conter itens vazios")
            @Size(max = 2000, message = "Cada criterio de pontuacao deve ter no maximo 2000 caracteres")
            String> criteriosPontuacao;

    @Size(max = 10, message = "A rubrica deve ter no maximo 10 erros graves")
    private List<
            @NotBlank(message = "Os erros graves nao podem conter itens vazios")
            @Size(max = 2000, message = "Cada erro grave deve ter no maximo 2000 caracteres")
            String> errosGraves;

    @Size(max = 10, message = "A rubrica deve ter no maximo 10 justificativas")
    private List<
            @NotBlank(message = "As justificativas nao podem conter itens vazios")
            @Size(max = 2000, message = "Cada justificativa deve ter no maximo 2000 caracteres")
            String> justificativas;

    @Size(max = 10, message = "A rubrica deve ter no maximo 10 prioridades")
    private List<
            @NotBlank(message = "As prioridades nao podem conter itens vazios")
            @Size(max = 2000, message = "Cada prioridade deve ter no maximo 2000 caracteres")
            String> prioridades;

    @Size(max = 10, message = "A rubrica deve ter no maximo 10 sinais de escalonamento")
    private List<
            @NotBlank(message = "Os sinais de escalonamento nao podem conter itens vazios")
            @Size(max = 2000, message = "Cada sinal de escalonamento deve ter no maximo 2000 caracteres")
            String> sinaisEscalonamento;
}
