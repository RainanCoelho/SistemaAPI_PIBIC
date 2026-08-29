package com.SistemaApiCrud.SistemaCrud;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.SistemaApiCrud.SistemaCrud.dto.AlternativaPerguntaDTO;
import com.SistemaApiCrud.SistemaCrud.dto.PerguntaRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.RubricaPerguntaDTO;
import com.SistemaApiCrud.SistemaCrud.entity.enums.TipoPergunta;
import com.SistemaApiCrud.SistemaCrud.exception.BadRequestException;
import com.SistemaApiCrud.SistemaCrud.mapper.PerguntaMapper;
import com.SistemaApiCrud.SistemaCrud.repository.AlternativaPerguntaRepository;
import com.SistemaApiCrud.SistemaCrud.repository.CasoClinicoRepository;
import com.SistemaApiCrud.SistemaCrud.repository.ConteudoClinicoRepository;
import com.SistemaApiCrud.SistemaCrud.repository.PacienteRepository;
import com.SistemaApiCrud.SistemaCrud.repository.PerguntaRepository;
import com.SistemaApiCrud.SistemaCrud.service.CasoClinicoLockService;
import com.SistemaApiCrud.SistemaCrud.service.PerguntaService;

class PerguntaServiceValidationTests {

    private final PerguntaRepository perguntaRepository = mock(PerguntaRepository.class);
    private final CasoClinicoRepository casoRepository = mock(CasoClinicoRepository.class);
    private final AlternativaPerguntaRepository alternativaRepository =
            mock(AlternativaPerguntaRepository.class);
    private final ConteudoClinicoRepository conteudoRepository =
            mock(ConteudoClinicoRepository.class);
    private final PacienteRepository pacienteRepository = mock(PacienteRepository.class);
    private final PerguntaMapper mapper = mock(PerguntaMapper.class);
    private final CasoClinicoLockService casoLockService = mock(CasoClinicoLockService.class);
    private final PerguntaService service = new PerguntaService(
            perguntaRepository,
            casoRepository,
            alternativaRepository,
            conteudoRepository,
            pacienteRepository,
            mapper,
            casoLockService);

    @Test
    void deveRejeitarAlternativasEmTipoQueNaoAsUtiliza() {
        PerguntaRequestDTO pergunta = pergunta(TipoPergunta.DISCURSIVA, "REVISAO_MANUAL");
        pergunta.setAlternativas(List.of(
                new AlternativaPerguntaDTO(null, "A", "Alternativa indevida", true)));

        assertThatThrownBy(() -> service.salvarEmCaso(1L, pergunta))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("nao aceita alternativas");

        verifyNoInteractions(casoLockService, perguntaRepository, alternativaRepository);
    }

    @Test
    void deveRejeitarMaisDeCincoAlternativasNaMultiplaEscolha() {
        PerguntaRequestDTO pergunta = pergunta(TipoPergunta.MULTIPLA_ESCOLHA, "A");
        pergunta.setAlternativas(List.of(
                new AlternativaPerguntaDTO(null, "A", "Alternativa A", true),
                new AlternativaPerguntaDTO(null, "B", "Alternativa B", false),
                new AlternativaPerguntaDTO(null, "C", "Alternativa C", false),
                new AlternativaPerguntaDTO(null, "D", "Alternativa D", false),
                new AlternativaPerguntaDTO(null, "E", "Alternativa E", false),
                new AlternativaPerguntaDTO(null, "F", "Alternativa F", false)));

        assertThatThrownBy(() -> service.salvarEmCaso(1L, pergunta))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("entre duas e cinco alternativas");
    }

    @Test
    void deveRejeitarGabaritoInvalidoDeVerdadeiroOuFalso() {
        PerguntaRequestDTO pergunta = pergunta(TipoPergunta.VERDADEIRO_FALSO, "DEPENDE");

        assertThatThrownBy(() -> service.salvarEmCaso(1L, pergunta))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("VERDADEIRO ou FALSO");
    }

    @Test
    void deveRejeitarDiagnosticoParaNovaPergunta() {
        PerguntaRequestDTO pergunta = pergunta(
                TipoPergunta.DIAGNOSTICO,
                "Pneumonia comunitaria");

        assertThatThrownBy(() -> service.salvarEmCaso(1L, pergunta))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("temporariamente indisponiveis");
    }

    @Test
    void deveRejeitarCondutaClinicaParaNovaPergunta() {
        PerguntaRequestDTO pergunta = pergunta(TipoPergunta.CONDUTA_CLINICA, "REVISAO_MANUAL");

        assertThatThrownBy(() -> service.salvarEmCaso(1L, pergunta))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("temporariamente indisponiveis");
    }

    @Test
    void deveRejeitarRubricaEstruturadaEmPerguntaObjetiva() {
        PerguntaRequestDTO pergunta = pergunta(TipoPergunta.MULTIPLA_ESCOLHA, "A");
        pergunta.setRubrica(rubricaDiscursiva());

        assertThatThrownBy(() -> service.salvarEmCaso(1L, pergunta))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Somente perguntas discursivas e de conduta");

        verifyNoInteractions(casoLockService, perguntaRepository, alternativaRepository);
    }

    private PerguntaRequestDTO pergunta(TipoPergunta tipo, String gabarito) {
        PerguntaRequestDTO pergunta = new PerguntaRequestDTO();
        pergunta.setIdCaso(1L);
        pergunta.setTexto("Enunciado clinico valido");
        pergunta.setResposta("Explicacao ou rubrica valida");
        pergunta.setTipo(tipo);
        pergunta.setGabarito(gabarito);
        pergunta.setAlternativas(List.of());
        return pergunta;
    }

    private RubricaPerguntaDTO rubricaDiscursiva() {
        return new RubricaPerguntaDTO(
                List.of("Integrar sinais e sintomas"),
                List.of("Dois pontos pela justificativa"),
                List.of("Ignorar sinal de gravidade"),
                List.of("Relacionar achados e hipotese"),
                null,
                null);
    }
}
