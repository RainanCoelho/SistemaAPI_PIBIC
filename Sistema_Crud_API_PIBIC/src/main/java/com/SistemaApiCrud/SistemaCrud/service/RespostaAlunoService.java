package com.SistemaApiCrud.SistemaCrud.service;

import java.text.Normalizer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.SistemaApiCrud.SistemaCrud.dto.DesempenhoAlunoDTO;
import com.SistemaApiCrud.SistemaCrud.dto.HistoricoAlunoDTO;
import com.SistemaApiCrud.SistemaCrud.dto.RelatorioDesempenhoProfessorDTO;
import com.SistemaApiCrud.SistemaCrud.dto.RevisaoRespostaDTO;
import com.SistemaApiCrud.SistemaCrud.dto.ResponderCasoRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.RespostaAlunoDTO;
import com.SistemaApiCrud.SistemaCrud.dto.RespostaPerguntaRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.ResultadoCasoDTO;
import com.SistemaApiCrud.SistemaCrud.entity.AlternativaPergunta;
import com.SistemaApiCrud.SistemaCrud.entity.Aluno;
import com.SistemaApiCrud.SistemaCrud.entity.RespostaAluno;
import com.SistemaApiCrud.SistemaCrud.entity.RevisaoRespostaAluno;
import com.SistemaApiCrud.SistemaCrud.entity.Usuario;
import com.SistemaApiCrud.SistemaCrud.entity.CasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.enums.StatusCasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.enums.TipoPergunta;
import com.SistemaApiCrud.SistemaCrud.entity.Pergunta;
import com.SistemaApiCrud.SistemaCrud.exception.BadRequestException;
import com.SistemaApiCrud.SistemaCrud.exception.BusinessException;
import com.SistemaApiCrud.SistemaCrud.exception.RecursoNaoEncontradoException;
import com.SistemaApiCrud.SistemaCrud.repository.AlternativaPerguntaRepository;
import com.SistemaApiCrud.SistemaCrud.repository.AlunoRepository;
import com.SistemaApiCrud.SistemaCrud.repository.CasoClinicoRepository;
import com.SistemaApiCrud.SistemaCrud.repository.PerguntaRepository;
import com.SistemaApiCrud.SistemaCrud.repository.ProfessorRepository;
import com.SistemaApiCrud.SistemaCrud.repository.RespostaAlunoRepository;
import com.SistemaApiCrud.SistemaCrud.repository.RevisaoRespostaAlunoRepository;
import com.SistemaApiCrud.SistemaCrud.repository.UsuarioRepository;

@Service
public class RespostaAlunoService {

    private final RespostaAlunoRepository repository;
    private final AlunoRepository alunoRepository;
    private final CasoClinicoRepository casoRepository;
    private final PerguntaRepository perguntaRepository;
    private final AlternativaPerguntaRepository alternativaRepository;
    private final ProfessorRepository professorRepository;
    private final TentativaCasoService tentativaCasoService;
    private final RevisaoRespostaAlunoRepository revisaoRepository;
    private final UsuarioRepository usuarioRepository;

    public RespostaAlunoService(
            RespostaAlunoRepository repository,
            AlunoRepository alunoRepository,
            CasoClinicoRepository casoRepository,
            PerguntaRepository perguntaRepository,
            AlternativaPerguntaRepository alternativaRepository,
            ProfessorRepository professorRepository,
            TentativaCasoService tentativaCasoService,
            RevisaoRespostaAlunoRepository revisaoRepository,
            UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.alunoRepository = alunoRepository;
        this.casoRepository = casoRepository;
        this.perguntaRepository = perguntaRepository;
        this.alternativaRepository = alternativaRepository;
        this.professorRepository = professorRepository;
        this.tentativaCasoService = tentativaCasoService;
        this.revisaoRepository = revisaoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public ResultadoCasoDTO responderCaso(Long idAluno, Long idCaso, ResponderCasoRequestDTO request) {
        Aluno aluno = alunoRepository.findById(idAluno)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno nao encontrado"));

        CasoClinico caso = casoRepository.findById(idCaso)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Caso clinico nao encontrado"));

        if (caso.getStatus() != StatusCasoClinico.PUBLICADO) {
            throw new BusinessException("O caso clinico ainda nao esta publicado");
        }

        var tentativa = tentativaCasoService.validarPrazo(idAluno, idCaso);
        if (repository.existsByAlunoIdAlunoAndCasoClinicoIdCaso(idAluno, idCaso)) {
            throw new BusinessException("O aluno ja respondeu este caso clinico");
        }
        Set<Long> idsRecebidos = validarRespostasCompletas(idCaso, request);
        List<Pergunta> perguntasDoCaso = perguntaRepository
                .findByCasoClinicoIdCasoAndIdIn(idCaso, idsRecebidos);
        if (perguntasDoCaso.size() != idsRecebidos.size()) {
            throw new BadRequestException(
                    "Todas as perguntas do caso devem ser respondidas exatamente uma vez");
        }
        Map<Long, Pergunta> perguntasPorId = perguntasDoCaso.stream()
                .collect(Collectors.toMap(Pergunta::getId, Function.identity()));
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

    private Set<Long> validarRespostasCompletas(
            Long idCaso,
            ResponderCasoRequestDTO request) {
        long quantidadePerguntas = perguntaRepository.countByCasoClinicoIdCaso(idCaso);
        if (quantidadePerguntas == 0) {
            throw new BusinessException("O caso clinico nao possui perguntas");
        }

        Set<Long> idsRecebidos = request.getRespostas()
                .stream()
                .map(RespostaPerguntaRequestDTO::getIdPergunta)
                .collect(Collectors.toSet());

        if (idsRecebidos.size() != request.getRespostas().size()) {
            throw new BadRequestException("Cada pergunta deve ser respondida uma unica vez");
        }

        if (idsRecebidos.size() != quantidadePerguntas) {
            throw new BadRequestException("Todas as perguntas do caso devem ser respondidas exatamente uma vez");
        }
        return idsRecebidos;
    }

    public HistoricoAlunoDTO buscarHistorico(Long idAluno, Pageable pageable) {
        if (!alunoRepository.existsById(idAluno)) {
            throw new RecursoNaoEncontradoException("Aluno nao encontrado");
        }

        Page<RespostaAlunoDTO> respostas = repository
                .findByAlunoIdAlunoOrderByDataRespostaDesc(idAluno, pageable)
                .map(this::paraDTO);

        return new HistoricoAlunoDTO(idAluno, respostas);
    }

    public DesempenhoAlunoDTO buscarDesempenho(Long idAluno) {
        if (!alunoRepository.existsById(idAluno)) {
            throw new RecursoNaoEncontradoException("Aluno nao encontrado");
        }

        long totalRespostas = repository.countByAlunoIdAluno(idAluno);
        long avaliadas = repository.contarAvaliadasPorAluno(idAluno);
        long pendentesRevisao = totalRespostas - avaliadas;
        long corretas = repository.countByAlunoIdAlunoAndCorretaTrue(idAluno);

        return new DesempenhoAlunoDTO(
                idAluno,
                totalRespostas,
                avaliadas,
                pendentesRevisao,
                corretas,
                calcularAproveitamento(avaliadas, corretas));
    }

    public RelatorioDesempenhoProfessorDTO gerarRelatorioProfessor(Long idProfessor) {
        if (!professorRepository.existsById(idProfessor)) {
            throw new RecursoNaoEncontradoException("Professor nao encontrado");
        }

        long totalRespostas = repository.countByCasoClinicoProfessorId(idProfessor);
        long avaliadas = repository.contarAvaliadasPorProfessor(idProfessor);
        long pendentesRevisao = totalRespostas - avaliadas;
        long corretas = repository.countByCasoClinicoProfessorIdAndCorretaTrue(idProfessor);

        return new RelatorioDesempenhoProfessorDTO(
                idProfessor,
                totalRespostas,
                avaliadas,
                pendentesRevisao,
                corretas,
                calcularAproveitamento(avaliadas, corretas));
    }

    public Page<RespostaAlunoDTO> listarPendentesRevisao(
            Long idCaso,
            Pageable paginacao) {
        if (!casoRepository.existsById(idCaso)) {
            throw new RecursoNaoEncontradoException("Caso clinico nao encontrado");
        }
        return repository.listarPendentesRevisaoPorCaso(idCaso, paginacao)
                .map(this::paraDTO);
    }

    @Transactional
    public RevisaoRespostaDTO revisarResposta(
            Long idCaso,
            Long idResposta,
            Boolean correta,
            String justificativa,
            Long idRevisor) {
        RespostaAluno resposta = repository.buscarPorIdParaAtualizacao(idResposta)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Resposta do aluno nao encontrada"));
        validarRespostaDoCaso(idCaso, resposta);

        Pergunta perguntaRespondida = resposta.getPergunta();
        TipoPergunta tipo = perguntaRespondida == null ? null : perguntaRespondida.getTipo();
        if (tipo != TipoPergunta.DISCURSIVA && tipo != TipoPergunta.CONDUTA_CLINICA) {
            throw new BusinessException(
                    "Somente respostas discursivas ou de conduta aceitam revisao humana");
        }

        if (justificativa == null || justificativa.isBlank()) {
            throw new BadRequestException("A justificativa da revisao e obrigatoria");
        }
        Usuario revisor = usuarioRepository.findById(idRevisor)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuario revisor nao encontrado"));
        String justificativaNormalizada = justificativa.trim();
        var ultimaRevisao = revisaoRepository
                .findFirstByRespostaIdOrderByVersaoRevisaoDesc(idResposta);
        if (ultimaRevisao.isPresent()
                && mesmaRevisao(
                        ultimaRevisao.get(),
                        revisor,
                        correta,
                        justificativaNormalizada)) {
            return paraRevisaoDTO(ultimaRevisao.get());
        }

        long novaVersao = resposta.getVersaoRevisao() + 1;
        Instant agora = Instant.now().truncatedTo(ChronoUnit.MICROS);
        resposta.setCorreta(correta);
        resposta.setRevisor(revisor);
        resposta.setDataRevisao(agora);
        resposta.setJustificativaRevisao(justificativaNormalizada);
        resposta.setVersaoRevisao(novaVersao);
        repository.save(resposta);

        RevisaoRespostaAluno revisao = new RevisaoRespostaAluno();
        revisao.setResposta(resposta);
        revisao.setRevisor(revisor);
        revisao.setCorreta(correta);
        revisao.setJustificativa(justificativaNormalizada);
        revisao.setDataRevisao(agora);
        revisao.setVersaoRevisao(novaVersao);
        return paraRevisaoDTO(revisaoRepository.save(revisao));
    }

    @Transactional(readOnly = true)
    public List<RevisaoRespostaDTO> listarHistoricoRevisoes(
            Long idCaso,
            Long idResposta) {
        RespostaAluno resposta = buscarRespostaDoCaso(idCaso, idResposta);
        return revisaoRepository
                .findByRespostaIdOrderByVersaoRevisaoAsc(resposta.getId())
                .stream()
                .map(this::paraRevisaoDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<RevisaoRespostaDTO> listarHistoricoRevisoesPaginado(
            Long idCaso,
            Long idResposta,
            Pageable paginacao) {
        RespostaAluno resposta = buscarRespostaDoCaso(idCaso, idResposta);
        return revisaoRepository
                .findByRespostaIdOrderByVersaoRevisaoAsc(resposta.getId(), paginacao)
                .map(this::paraRevisaoDTO);
    }

    private RespostaAluno buscarRespostaDoCaso(Long idCaso, Long idResposta) {
        RespostaAluno resposta = repository.findById(idResposta)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Resposta do aluno nao encontrada"));
        validarRespostaDoCaso(idCaso, resposta);
        return resposta;
    }

    private void validarRespostaDoCaso(Long idCaso, RespostaAluno resposta) {
        if (resposta.getCasoClinico() == null
                || !idCaso.equals(resposta.getCasoClinico().getIdCaso())) {
            throw new BadRequestException(
                    "A resposta informada nao pertence ao caso clinico");
        }
    }

    private boolean mesmaRevisao(
            RevisaoRespostaAluno revisao,
            Usuario revisor,
            Boolean correta,
            String justificativa) {
        return revisao.getRevisor() != null
                && revisao.getRevisor().getId().equals(revisor.getId())
                && revisao.getCorreta().equals(correta)
                && revisao.getJustificativa().equals(justificativa);
    }

    private RevisaoRespostaDTO paraRevisaoDTO(RevisaoRespostaAluno revisao) {
        return new RevisaoRespostaDTO(
                revisao.getId(),
                revisao.getResposta().getId(),
                revisao.getRevisor().getId(),
                revisao.getCorreta(),
                revisao.getJustificativa(),
                revisao.getDataRevisao(),
                revisao.getVersaoRevisao());
    }

    private RespostaAluno criarResposta(
            Aluno aluno,
            CasoClinico caso,
            RespostaPerguntaRequestDTO respostaRequest,
            Map<Long, Pergunta> perguntasPorId,
            Map<Long, List<AlternativaPergunta>> alternativasPorPergunta) {
        Pergunta pergunta = perguntasPorId.get(respostaRequest.getIdPergunta());
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
                alternativasPorPergunta.getOrDefault(pergunta.getId(), List.of()))
                .orElse(null));

        return respostaAluno;
    }

    private Map<Long, List<AlternativaPergunta>> buscarAlternativasPorPergunta(
            List<Pergunta> perguntas) {
        List<Long> ids = perguntas.stream().map(Pergunta::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        return alternativaRepository.findByPerguntaIdInOrderByPerguntaIdAscLetraAsc(ids)
                .stream()
                .collect(Collectors.groupingBy(alternativa -> alternativa.getPergunta().getId()));
    }

    private Optional<Boolean> compararResposta(
            Pergunta pergunta,
            String respostaMarcada,
            List<AlternativaPergunta> alternativas) {
        TipoPergunta tipo = pergunta.getTipo();
        if (tipo == null) {
            return Optional.of(false);
        }

        return switch (tipo) {
            case MULTIPLA_ESCOLHA ->
                Optional.of(compararMultiplaEscolha(pergunta, respostaMarcada, alternativas));
            case VERDADEIRO_FALSO ->
                Optional.of(compararVerdadeiroOuFalso(pergunta, respostaMarcada));
            case DIAGNOSTICO ->
                Optional.of(compararDiagnostico(pergunta, respostaMarcada));
            case DISCURSIVA, CONDUTA_CLINICA -> Optional.empty();
        };
    }

    private boolean compararMultiplaEscolha(
            Pergunta pergunta,
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
            Pergunta pergunta,
            String respostaMarcada) {
        String gabaritoNormalizado = normalizarAcentosEEspacos(obterGabarito(pergunta));
        String respostaNormalizada = normalizarAcentosEEspacos(respostaMarcada);

        return eVerdadeiroOuFalso(gabaritoNormalizado)
                && eVerdadeiroOuFalso(respostaNormalizada)
                && gabaritoNormalizado.equals(respostaNormalizada);
    }

    private boolean compararDiagnostico(
            Pergunta pergunta,
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

    private String obterGabarito(Pergunta pergunta) {
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

    private ResultadoCasoDTO montarResultado(Long idAluno, Long idCaso, List<RespostaAluno> respostas) {
        int totalRespondidas = respostas.size();
        int avaliadas = (int) respostas.stream()
                .filter(resposta -> resposta.getCorreta() != null)
                .count();
        int pendentesRevisao = totalRespondidas - avaliadas;
        int corretas = (int) respostas.stream().filter(resposta -> Boolean.TRUE.equals(resposta.getCorreta())).count();

        List<RespostaAlunoDTO> respostasDTO = respostas.stream()
                .map(this::paraDTO)
                .toList();

        return new ResultadoCasoDTO(
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

    private RespostaAlunoDTO paraDTO(RespostaAluno resposta) {
        RespostaAlunoDTO dto = new RespostaAlunoDTO();
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
