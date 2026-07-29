package com.SistemaApiCrud.SistemaCrud.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.SistemaApiCrud.SistemaCrud.DTO.desempenho_aluno_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.historico_aluno_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.relatorio_desempenho_professor_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.responder_caso_request_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.resposta_aluno_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.resposta_pergunta_request_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.resultado_caso_DTO;
import com.SistemaApiCrud.SistemaCrud.entity.AlternativaPergunta;
import com.SistemaApiCrud.SistemaCrud.entity.Aluno;
import com.SistemaApiCrud.SistemaCrud.entity.RespostaAluno;
import com.SistemaApiCrud.SistemaCrud.entity.casos_clinicos;
import com.SistemaApiCrud.SistemaCrud.entity.enums.StatusCasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.enums.TipoPergunta;
import com.SistemaApiCrud.SistemaCrud.entity.pergunta;
import com.SistemaApiCrud.SistemaCrud.exception.BadRequestException;
import com.SistemaApiCrud.SistemaCrud.exception.BusinessException;
import com.SistemaApiCrud.SistemaCrud.exception.ConflitoEstadoException;
import com.SistemaApiCrud.SistemaCrud.exception.RecursoNaoEncontradoException;
import com.SistemaApiCrud.SistemaCrud.repository.alternativa_pergunta_repository;
import com.SistemaApiCrud.SistemaCrud.repository.aluno_repository;
import com.SistemaApiCrud.SistemaCrud.repository.caso_clinico_repository;
import com.SistemaApiCrud.SistemaCrud.repository.pergunta_repository;
import com.SistemaApiCrud.SistemaCrud.repository.professor_repository;
import com.SistemaApiCrud.SistemaCrud.repository.resposta_aluno_repository;

@Service
public class resposta_aluno_service {

    private final resposta_aluno_repository repository;
    private final aluno_repository alunoRepository;
    private final caso_clinico_repository casoRepository;
    private final pergunta_repository perguntaRepository;
    private final alternativa_pergunta_repository alternativaRepository;
    private final professor_repository professorRepository;
    private final TentativaCasoService tentativaCasoService;

    public resposta_aluno_service(
            resposta_aluno_repository repository,
            aluno_repository alunoRepository,
            caso_clinico_repository casoRepository,
            pergunta_repository perguntaRepository,
            alternativa_pergunta_repository alternativaRepository,
            professor_repository professorRepository,
            TentativaCasoService tentativaCasoService) {
        this.repository = repository;
        this.alunoRepository = alunoRepository;
        this.casoRepository = casoRepository;
        this.perguntaRepository = perguntaRepository;
        this.alternativaRepository = alternativaRepository;
        this.professorRepository = professorRepository;
        this.tentativaCasoService = tentativaCasoService;
    }

    @Transactional
    public resultado_caso_DTO responderCaso(Long idAluno, Long idCaso, responder_caso_request_DTO request) {
        Aluno aluno = alunoRepository.findById(idAluno)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno nao encontrado"));

        casos_clinicos caso = casoRepository.findById(idCaso)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Caso clinico nao encontrado"));

        if (caso.getStatus() != StatusCasoClinico.PUBLICADO) {
            throw new BusinessException("O caso clinico ainda nao esta publicado");
        }

        if (repository.existsByAlunoIdAlunoAndCasoClinicoIdCaso(idAluno, idCaso)) {
            throw new BusinessException("O aluno ja respondeu este caso clinico");
        }

        var tentativa = tentativaCasoService.validarPrazo(idAluno, idCaso);
        List<pergunta> perguntasDoCaso = perguntaRepository.findByCasoClinicoIdCaso(idCaso);
        validarRespostasCompletas(perguntasDoCaso, request);
        Map<Long, pergunta> perguntasPorId = perguntasDoCaso.stream()
                .collect(Collectors.toMap(pergunta::getId, Function.identity()));
        Map<Long, List<AlternativaPergunta>> alternativasPorPergunta =
                buscarAlternativasPorPergunta(perguntasDoCaso);

        List<RespostaAluno> respostas = request.getRespostas()
                .stream()
                .map(resposta -> criarResposta(
                        aluno,
                        caso,
                        resposta,
                        perguntasPorId,
                        alternativasPorPergunta))
                .toList();

        List<RespostaAluno> respostasSalvas = repository.saveAll(respostas);
        tentativaCasoService.finalizar(tentativa);
        return montarResultado(idAluno, idCaso, respostasSalvas);
    }

    private void validarRespostasCompletas(
            List<pergunta> perguntasDoCaso,
            responder_caso_request_DTO request) {
        if (perguntasDoCaso.isEmpty()) {
            throw new BusinessException("O caso clinico nao possui perguntas");
        }

        Set<Long> idsRecebidos = request.getRespostas()
                .stream()
                .map(resposta_pergunta_request_DTO::getIdPergunta)
                .collect(Collectors.toSet());

        if (idsRecebidos.size() != request.getRespostas().size()) {
            throw new BadRequestException("Cada pergunta deve ser respondida uma unica vez");
        }

        Set<Long> idsEsperados = perguntasDoCaso.stream()
                .map(pergunta::getId)
                .collect(Collectors.toSet());

        if (!idsRecebidos.equals(idsEsperados)) {
            throw new BadRequestException("Todas as perguntas do caso devem ser respondidas exatamente uma vez");
        }
    }

    public historico_aluno_DTO buscarHistorico(Long idAluno, Pageable pageable) {
        if (!alunoRepository.existsById(idAluno)) {
            throw new RecursoNaoEncontradoException("Aluno nao encontrado");
        }

        Page<resposta_aluno_DTO> respostas = repository
                .findByAlunoIdAlunoOrderByDataRespostaDesc(idAluno, pageable)
                .map(this::paraDTO);

        return new historico_aluno_DTO(idAluno, respostas);
    }

    public desempenho_aluno_DTO buscarDesempenho(Long idAluno) {
        if (!alunoRepository.existsById(idAluno)) {
            throw new RecursoNaoEncontradoException("Aluno nao encontrado");
        }

        long totalRespostas = repository.countByAlunoIdAluno(idAluno);
        long avaliadas = repository.contarAvaliadasPorAluno(idAluno);
        long pendentesRevisao = totalRespostas - avaliadas;
        long corretas = repository.countByAlunoIdAlunoAndCorretaTrue(idAluno);

        return new desempenho_aluno_DTO(
                idAluno,
                totalRespostas,
                avaliadas,
                pendentesRevisao,
                corretas,
                calcularAproveitamento(avaliadas, corretas));
    }

    public relatorio_desempenho_professor_DTO gerarRelatorioProfessor(Long idProfessor) {
        if (!professorRepository.existsById(idProfessor)) {
            throw new RecursoNaoEncontradoException("Professor nao encontrado");
        }

        long totalRespostas = repository.countByCasoClinicoProfessorId(idProfessor);
        long avaliadas = repository.contarAvaliadasPorProfessor(idProfessor);
        long pendentesRevisao = totalRespostas - avaliadas;
        long corretas = repository.countByCasoClinicoProfessorIdAndCorretaTrue(idProfessor);

        return new relatorio_desempenho_professor_DTO(
                idProfessor,
                totalRespostas,
                avaliadas,
                pendentesRevisao,
                corretas,
                calcularAproveitamento(avaliadas, corretas));
    }

    public Page<resposta_aluno_DTO> listarPendentesRevisao(
            Long idCaso,
            Pageable paginacao) {
        if (!casoRepository.existsById(idCaso)) {
            throw new RecursoNaoEncontradoException("Caso clinico nao encontrado");
        }
        return repository.listarPendentesRevisaoPorCaso(idCaso, paginacao)
                .map(this::paraDTO);
    }

    @Transactional
    public resposta_aluno_DTO revisarResposta(
            Long idCaso,
            Long idResposta,
            Boolean correta) {
        RespostaAluno resposta = repository.buscarPorIdParaAtualizacao(idResposta)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Resposta do aluno nao encontrada"));
        if (resposta.getCasoClinico() == null
                || !idCaso.equals(resposta.getCasoClinico().getIdCaso())) {
            throw new BadRequestException(
                    "A resposta informada nao pertence ao caso clinico");
        }

        pergunta perguntaRespondida = resposta.getPergunta();
        TipoPergunta tipo = perguntaRespondida == null ? null : perguntaRespondida.getTipo();
        if (tipo != TipoPergunta.DISCURSIVA && tipo != TipoPergunta.CONDUTA_CLINICA) {
            throw new BusinessException(
                    "Somente respostas discursivas ou de conduta aceitam revisao humana");
        }

        if (resposta.getCorreta() != null) {
            if (resposta.getCorreta().equals(correta)) {
                return paraDTO(resposta);
            }
            throw new ConflitoEstadoException(
                    "A resposta ja foi revisada com um resultado diferente");
        }

        resposta.setCorreta(correta);
        return paraDTO(repository.save(resposta));
    }

    private RespostaAluno criarResposta(
            Aluno aluno,
            casos_clinicos caso,
            resposta_pergunta_request_DTO respostaRequest,
            Map<Long, pergunta> perguntasPorId,
            Map<Long, List<AlternativaPergunta>> alternativasPorPergunta) {
        pergunta pergunta = perguntasPorId.get(respostaRequest.getIdPergunta());
        if (pergunta == null
                || pergunta.getCasoClinico() == null
                || !pergunta.getCasoClinico().getIdCaso().equals(caso.getIdCaso())) {
            throw new BadRequestException("A pergunta informada nao pertence ao caso clinico");
        }

        RespostaAluno respostaAluno = new RespostaAluno();
        respostaAluno.setAluno(aluno);
        respostaAluno.setCasoClinico(caso);
        respostaAluno.setPergunta(pergunta);
        respostaAluno.setRespostaMarcada(respostaRequest.getRespostaMarcada());
        respostaAluno.setCorreta(compararResposta(
                pergunta,
                respostaRequest.getRespostaMarcada(),
                alternativasPorPergunta.getOrDefault(pergunta.getId(), List.of())));

        return respostaAluno;
    }

    private Map<Long, List<AlternativaPergunta>> buscarAlternativasPorPergunta(
            List<pergunta> perguntas) {
        List<Long> ids = perguntas.stream().map(pergunta::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        return alternativaRepository.findByPerguntaIdInOrderByPerguntaIdAscLetraAsc(ids)
                .stream()
                .collect(Collectors.groupingBy(alternativa -> alternativa.getPergunta().getId()));
    }

    private Boolean compararResposta(
            pergunta pergunta,
            String respostaMarcada,
            List<AlternativaPergunta> alternativas) {
        TipoPergunta tipo = pergunta.getTipo();
        if (tipo == null) {
            return false;
        }

        return switch (tipo) {
            case MULTIPLA_ESCOLHA ->
                compararMultiplaEscolha(pergunta, respostaMarcada, alternativas);
            case VERDADEIRO_FALSO ->
                compararVerdadeiroOuFalso(pergunta, respostaMarcada);
            case DIAGNOSTICO ->
                compararDiagnostico(pergunta, respostaMarcada);
            case DISCURSIVA, CONDUTA_CLINICA -> null;
        };
    }

    private boolean compararMultiplaEscolha(
            pergunta pergunta,
            String respostaMarcada,
            List<AlternativaPergunta> alternativas) {
        if (!alternativas.isEmpty()) {
            return alternativas.stream()
                    .anyMatch(alternativa -> Boolean.TRUE.equals(alternativa.getCorreta())
                            && correspondeResposta(alternativa, respostaMarcada));
        }

        String gabarito = obterGabarito(pergunta);
        return gabarito != null && respostaMarcada != null && gabarito.trim().equalsIgnoreCase(respostaMarcada.trim());
    }

    private boolean compararVerdadeiroOuFalso(
            pergunta pergunta,
            String respostaMarcada) {
        String gabaritoNormalizado = normalizarAcentosEEspacos(obterGabarito(pergunta));
        String respostaNormalizada = normalizarAcentosEEspacos(respostaMarcada);

        return eVerdadeiroOuFalso(gabaritoNormalizado)
                && eVerdadeiroOuFalso(respostaNormalizada)
                && gabaritoNormalizado.equals(respostaNormalizada);
    }

    private boolean compararDiagnostico(
            pergunta pergunta,
            String respostaMarcada) {
        String gabarito = obterGabarito(pergunta);
        String respostaNormalizada = normalizarDiagnostico(respostaMarcada);
        if (gabarito == null || respostaNormalizada.isBlank()) {
            return false;
        }

        return List.of(gabarito.split("\\|"))
                .stream()
                .map(this::normalizarDiagnostico)
                .filter(alias -> !alias.isBlank())
                .anyMatch(alias -> alias.equals(respostaNormalizada));
    }

    private String obterGabarito(pergunta pergunta) {
        String gabarito = pergunta.getGabarito();
        return gabarito == null || gabarito.isBlank()
                ? pergunta.getResposta()
                : gabarito;
    }

    private boolean eVerdadeiroOuFalso(String valor) {
        return "verdadeiro".equals(valor) || "falso".equals(valor);
    }

    private String normalizarDiagnostico(String valor) {
        String valorNormalizado = normalizarAcentosEEspacos(valor)
                .replace('\u2212', '-')
                .replaceAll("(?<=[\\p{L}\\p{N}])\\s*\\+(?=\\s|$|[/,;])", " positivo ")
                .replaceAll("(?<=[\\p{L}\\p{N}])\\s*-(?=\\s|$|[/,;])", " negativo ");
        return valorNormalizado
                .replaceAll("[\\p{P}\\p{S}]+", " ")
                .replaceAll("\\s+", " ")
                .strip();
    }

    private String normalizarAcentosEEspacos(String valor) {
        if (valor == null) {
            return "";
        }

        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .strip();
    }

    private boolean correspondeResposta(AlternativaPergunta alternativa, String respostaMarcada) {
        if (respostaMarcada == null) {
            return false;
        }

        String resposta = respostaMarcada.trim();
        return corresponde(resposta, alternativa.getLetra()) || corresponde(resposta, alternativa.getTexto());
    }

    private boolean corresponde(String valor, String referencia) {
        return valor != null && referencia != null && valor.equalsIgnoreCase(referencia.trim());
    }

    private resultado_caso_DTO montarResultado(Long idAluno, Long idCaso, List<RespostaAluno> respostas) {
        int totalRespondidas = respostas.size();
        int avaliadas = (int) respostas.stream()
                .filter(resposta -> resposta.getCorreta() != null)
                .count();
        int pendentesRevisao = totalRespondidas - avaliadas;
        int corretas = (int) respostas.stream().filter(resposta -> Boolean.TRUE.equals(resposta.getCorreta())).count();

        List<resposta_aluno_DTO> respostasDTO = respostas.stream()
                .map(this::paraDTO)
                .toList();

        return new resultado_caso_DTO(
                idAluno,
                idCaso,
                totalRespondidas,
                avaliadas,
                pendentesRevisao,
                corretas,
                calcularAproveitamento(avaliadas, corretas),
                respostasDTO);
    }

    private Double calcularAproveitamento(long totalAvaliadas, long corretas) {
        if (totalAvaliadas == 0) {
            return 0.0;
        }

        return (corretas * 100.0) / totalAvaliadas;
    }

    private resposta_aluno_DTO paraDTO(RespostaAluno resposta) {
        resposta_aluno_DTO dto = new resposta_aluno_DTO();
        dto.setId(resposta.getId());

        if (resposta.getAluno() != null) {
            dto.setIdAluno(resposta.getAluno().getIdAluno());
        }

        if (resposta.getCasoClinico() != null) {
            dto.setIdCaso(resposta.getCasoClinico().getIdCaso());
        }

        if (resposta.getPergunta() != null) {
            dto.setIdPergunta(resposta.getPergunta().getId());
        }

        dto.setRespostaMarcada(resposta.getRespostaMarcada());
        dto.setCorreta(resposta.getCorreta());
        dto.setDataResposta(resposta.getDataResposta());

        return dto;
    }
}
