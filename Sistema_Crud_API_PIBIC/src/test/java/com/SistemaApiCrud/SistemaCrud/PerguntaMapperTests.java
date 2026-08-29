package com.SistemaApiCrud.SistemaCrud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.SistemaApiCrud.SistemaCrud.dto.PerguntaRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.PerguntaResponseDTO;
import com.SistemaApiCrud.SistemaCrud.dto.RubricaPerguntaDTO;
import com.SistemaApiCrud.SistemaCrud.entity.CasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.Pergunta;
import com.SistemaApiCrud.SistemaCrud.entity.enums.TipoPergunta;
import com.SistemaApiCrud.SistemaCrud.mapper.PerguntaMapper;

class PerguntaMapperTests {

    private final PerguntaMapper mapper = new PerguntaMapper();

    @Test
    void deveMapearRubricaEstruturadaNosDoisSentidos() {
        CasoClinico caso = new CasoClinico();
        caso.setIdCaso(7L);
        PerguntaRequestDTO request = new PerguntaRequestDTO();
        request.setTexto("Explique o raciocinio clinico.");
        request.setResposta("Resumo compativel com clientes antigos.");
        request.setGabarito("REVISAO_MANUAL");
        request.setTipo(TipoPergunta.DISCURSIVA);
        request.setRubrica(new RubricaPerguntaDTO(
                List.of("Integrar os achados clinicos"),
                List.of("Atribuir dois pontos pela justificativa"),
                List.of("Ignorar sinal de gravidade"),
                List.of("Relacionar hipotese e exame"),
                null,
                null));

        Pergunta entity = mapper.toEntity(request, caso);
        PerguntaResponseDTO response = mapper.toResponse(entity, List.of());

        assertThat(entity.getRubrica().getCriteriosEssenciais())
                .containsExactly("Integrar os achados clinicos");
        assertThat(response.getRubrica().getErrosGraves())
                .containsExactly("Ignorar sinal de gravidade");
        assertThat(response.getIdCaso()).isEqualTo(7L);
    }

    @Test
    void devePreservarRubricaNulaEmPerguntaLegada() {
        Pergunta entity = new Pergunta();
        entity.setTexto("Pergunta antiga");
        entity.setResposta("Rubrica textual antiga");
        entity.setGabarito("REVISAO_MANUAL");
        entity.setTipo(TipoPergunta.DISCURSIVA);

        assertThat(mapper.toResponse(entity, List.of()).getRubrica()).isNull();
    }
}
