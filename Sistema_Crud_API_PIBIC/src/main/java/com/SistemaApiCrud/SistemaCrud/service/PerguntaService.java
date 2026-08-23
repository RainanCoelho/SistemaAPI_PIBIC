package com.SistemaApiCrud.SistemaCrud.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.SistemaApiCrud.SistemaCrud.dto.AlternativaPerguntaDTO;
import com.SistemaApiCrud.SistemaCrud.dto.AlternativaAlunoDTO;
import com.SistemaApiCrud.SistemaCrud.dto.PerguntaAlunoDTO;
import com.SistemaApiCrud.SistemaCrud.dto.PerguntaRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.PerguntaResponseDTO;
import com.SistemaApiCrud.SistemaCrud.entity.AlternativaPergunta;
import com.SistemaApiCrud.SistemaCrud.entity.CasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.enums.TipoPergunta;
import com.SistemaApiCrud.SistemaCrud.entity.Pergunta;
import com.SistemaApiCrud.SistemaCrud.exception.BadRequestException;
import com.SistemaApiCrud.SistemaCrud.exception.ConflitoEstadoException;
import com.SistemaApiCrud.SistemaCrud.exception.RecursoNaoEncontradoException;
import com.SistemaApiCrud.SistemaCrud.mapper.PerguntaMapper;
import com.SistemaApiCrud.SistemaCrud.repository.AlternativaPerguntaRepository;
import com.SistemaApiCrud.SistemaCrud.repository.CasoClinicoRepository;
import com.SistemaApiCrud.SistemaCrud.repository.ConteudoClinicoRepository;
import com.SistemaApiCrud.SistemaCrud.repository.PacienteRepository;
import com.SistemaApiCrud.SistemaCrud.repository.PerguntaRepository;

@Service
public class PerguntaService {

    private static final Set<String> LETRAS_ALTERNATIVAS_PERMITIDAS =
            Set.of("A", "B", "C", "D", "E");

    private final PerguntaRepository repository;
    private final CasoClinicoRepository casoRepository;
    private final AlternativaPerguntaRepository alternativaRepository;
    private final ConteudoClinicoRepository conteudoRepository;
    private final PacienteRepository pacienteRepository;
    private final PerguntaMapper mapper;
    private final CasoClinicoLockService casoLockService;

    public PerguntaService(
            PerguntaRepository repository,
            CasoClinicoRepository casoRepository,
            AlternativaPerguntaRepository alternativaRepository,
            ConteudoClinicoRepository conteudoRepository,
            PacienteRepository pacienteRepository,
            PerguntaMapper mapper,
            CasoClinicoLockService casoLockService) {
        this.repository = repository;
        this.casoRepository = casoRepository;
        this.alternativaRepository = alternativaRepository;
        this.conteudoRepository = conteudoRepository;
        this.pacienteRepository = pacienteRepository;
        this.mapper = mapper;
        this.casoLockService = casoLockService;
    }

    public Page<PerguntaResponseDTO> listar(Pageable pageable) {
        Page<Pergunta> perguntas = repository.findAll(pageable);
        Map<Long, List<AlternativaPerguntaDTO>> alternativas = buscarAlternativasPorPergunta(
                perguntas.getContent());
        return perguntas.map(pergunta -> paraDTO(pergunta, alternativas));
    }

    public Page<PerguntaResponseDTO> listarPorProfessor(Long idProfessor, Pageable pageable) {
        Page<Pergunta> perguntas = repository.findByCasoClinicoProfessorId(idProfessor, pageable);
        Map<Long, List<AlternativaPerguntaDTO>> alternativas = buscarAlternativasPorPergunta(
                perguntas.getContent());
        return perguntas.map(pergunta -> paraDTO(pergunta, alternativas));
    }

    public PerguntaResponseDTO buscarPorId(Long id) {
        return paraDTO(buscarEntityPorId(id));
    }

    public Page<PerguntaResponseDTO> listarPorCaso(Long idCaso, Pageable pageable) {
        if (!casoRepository.existsById(idCaso)) {
            throw new RecursoNaoEncontradoException("Caso clinico nao encontrado");
        }

        Page<Pergunta> perguntas = repository.findByCasoClinicoIdCaso(idCaso, pageable);
        Map<Long, List<AlternativaPerguntaDTO>> alternativas = buscarAlternativasPorPergunta(
                perguntas.getContent());
        return perguntas.map(pergunta -> paraDTO(pergunta, alternativas));
    }

    public List<PerguntaResponseDTO> listarTodasPorCaso(Long idCaso) {
        List<Pergunta> perguntas = repository.findByCasoClinicoIdCaso(idCaso);
        Map<Long, List<AlternativaPerguntaDTO>> alternativas = buscarAlternativasPorPergunta(perguntas);
        return perguntas.stream()
                .map(pergunta -> paraDTO(pergunta, alternativas))
                .toList();
    }

    public List<PerguntaAlunoDTO> listarTodasParaAlunoPorCaso(Long idCaso) {
        List<Pergunta> perguntas = repository.findByCasoClinicoIdCaso(idCaso);
        Map<Long, List<AlternativaPerguntaDTO>> alternativas = buscarAlternativasPorPergunta(perguntas);
        return perguntas.stream()
                .map(pergunta -> paraAlunoDTO(pergunta, alternativas))
                .toList();
    }

    @Transactional
    public PerguntaResponseDTO salvar(PerguntaRequestDTO dto) {
        if (dto.getIdCaso() == null) {
            throw new BadRequestException("O caso clinico e obrigatorio");
        }

        return salvarNoCaso(dto.getIdCaso(), dto);
    }

    @Transactional
    public PerguntaResponseDTO salvarEmCaso(Long idCaso, PerguntaRequestDTO dto) {
        return salvarNoCaso(idCaso, dto);
    }

    private PerguntaResponseDTO salvarNoCaso(Long idCaso, PerguntaRequestDTO dto) {
        validarPergunta(dto);
        CasoClinico caso = casoLockService.bloquearRascunho(idCaso);
        Pergunta pergunta = mapper.toEntity(dto, caso);
        Pergunta perguntaSalva = repository.save(pergunta);
        List<AlternativaPergunta> alternativas = salvarAlternativas(perguntaSalva, dto);
        return mapper.toResponse(
                perguntaSalva,
                alternativas.stream().map(this::paraAlternativaDTO).toList());
    }

    @Transactional
    public List<PerguntaResponseDTO> salvarLoteEmCaso(
            Long idCaso,
            List<PerguntaRequestDTO> perguntasDTO) {
        return salvarLoteInterno(idCaso, perguntasDTO, null, null);
    }

    @Transactional
    public List<PerguntaResponseDTO> salvarLoteEmCaso(
            Long idCaso,
            List<PerguntaRequestDTO> perguntasDTO,
            String fingerprintEsperado) {
        return salvarLoteInterno(idCaso, perguntasDTO, fingerprintEsperado, null);
    }

    @Transactional
    public List<PerguntaResponseDTO> salvarLoteEmCaso(
            Long idCaso,
            List<PerguntaRequestDTO> perguntasDTO,
            String fingerprintEsperado,
            long quantidadePerguntasEsperada) {
        return salvarLoteInterno(
                idCaso,
                perguntasDTO,
                fingerprintEsperado,
                quantidadePerguntasEsperada);
    }

    private List<PerguntaResponseDTO> salvarLoteInterno(
            Long idCaso,
            List<PerguntaRequestDTO> perguntasDTO,
            String fingerprintEsperado,
            Long quantidadePerguntasEsperada) {
        if (perguntasDTO == null || perguntasDTO.isEmpty()) {
            throw new BadRequestException("Informe ao menos uma pergunta");
        }

        perguntasDTO.forEach(this::validarPergunta);
        CasoClinico caso = casoLockService.bloquearRascunho(idCaso);
        validarContextoInalterado(
                caso,
                fingerprintEsperado,
                quantidadePerguntasEsperada);
        validarEnunciadosGerados(
                caso.getIdCaso(),
                perguntasDTO,
                quantidadePerguntasEsperada);
        List<Pergunta> perguntas = perguntasDTO.stream()
                .map(dto -> mapper.toEntity(dto, caso))
                .toList();
        List<Pergunta> perguntasSalvas = repository.saveAll(perguntas);

        List<AlternativaPergunta> alternativas = new ArrayList<>();
        for (int indice = 0; indice < perguntasSalvas.size(); indice++) {
            Pergunta perguntaSalva = perguntasSalvas.get(indice);
            PerguntaRequestDTO dto = perguntasDTO.get(indice);
            alternativas.addAll(montarAlternativasDTO(dto).stream()
                    .map(alternativa -> paraAlternativaEntity(perguntaSalva, alternativa, dto))
                    .toList());
        }

        List<AlternativaPergunta> alternativasSalvas = alternativas.isEmpty()
                ? List.of()
                : alternativaRepository.saveAll(alternativas);
        Map<Long, List<AlternativaPerguntaDTO>> alternativasPorPergunta =
                agruparAlternativas(alternativasSalvas);

        return perguntasSalvas.stream()
                .map(pergunta -> paraDTO(pergunta, alternativasPorPergunta))
                .toList();
    }

    private void validarContextoInalterado(
            CasoClinico caso,
            String fingerprintEsperado,
            Long quantidadePerguntasEsperada) {
        if (quantidadePerguntasEsperada != null
                && repository.countByCasoClinicoIdCaso(caso.getIdCaso())
                        != quantidadePerguntasEsperada) {
            throw new ConflitoEstadoException(
                    "As perguntas do caso clinico mudaram durante a geracao; gere novamente");
        }

        if (fingerprintEsperado == null) {
            return;
        }

        var conteudoAtual = conteudoRepository
                .findFirstByCasoClinicoIdCasoOrderByIdConteudoDesc(caso.getIdCaso())
                .orElseThrow(() -> new ConflitoEstadoException(
                        "O conteudo do caso clinico mudou durante a geracao; gere as perguntas novamente"));
        var pacientesAtuais = pacienteRepository
                .findByCasoClinicoIdCasoOrderByIdPacienteAsc(caso.getIdCaso());
        String fingerprintAtual = CasoClinicoFingerprint.calcular(
                caso,
                conteudoAtual,
                pacientesAtuais);

        if (!fingerprintEsperado.equals(fingerprintAtual)) {
            throw new ConflitoEstadoException(
                    "O caso clinico mudou durante a geracao; gere as perguntas novamente");
        }
    }

    public long contarPorCaso(Long idCaso) {
        return repository.countByCasoClinicoIdCaso(idCaso);
    }

    private void validarEnunciadosGerados(
            Long idCaso,
            List<PerguntaRequestDTO> perguntasDTO,
            Long quantidadePerguntasEsperada) {
        if (quantidadePerguntasEsperada == null) {
            return;
        }

        Set<String> enunciados = repository.findTextosByCasoClinicoIdCaso(idCaso)
                .stream()
                .map(this::normalizarEnunciado)
                .collect(Collectors.toSet());
        for (PerguntaRequestDTO pergunta : perguntasDTO) {
            if (!enunciados.add(normalizarEnunciado(pergunta.getTexto()))) {
                throw new ConflitoEstadoException(
                        "A IA gerou uma pergunta que ja existe neste caso clinico");
            }
        }
    }

    private String normalizarEnunciado(String texto) {
        return texto.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    @Transactional
    public PerguntaResponseDTO atualizar(Long id, PerguntaRequestDTO dto) {
        Pergunta pergunta = buscarEntityPorIdParaAtualizacao(id);
        validarPergunta(dto);
        Long idCasoAtual = pergunta.getCasoClinico().getIdCaso();
        Long idCasoDestino = dto.getIdCaso() != null ? dto.getIdCaso() : idCasoAtual;
        Map<Long, CasoClinico> casosBloqueados = casoLockService.bloquearRascunhos(
                List.of(idCasoAtual, idCasoDestino));
        CasoClinico caso = dto.getIdCaso() != null
                ? casosBloqueados.get(idCasoDestino)
                : null;
        mapper.updateEntity(dto, pergunta, caso);

        Pergunta perguntaAtualizada = repository.save(pergunta);
        alternativaRepository.deleteByPerguntaId(id);
        alternativaRepository.flush();
        salvarAlternativas(perguntaAtualizada, dto);
        return paraDTO(perguntaAtualizada);
    }

    @Transactional
    public void deletar(Long id) {
        Pergunta pergunta = buscarEntityPorIdParaAtualizacao(id);
        casoLockService.bloquearRascunho(pergunta.getCasoClinico().getIdCaso());
        alternativaRepository.deleteByPerguntaId(id);
        repository.deleteById(id);
    }

    private PerguntaResponseDTO paraDTO(Pergunta pergunta) {
        return mapper.toResponse(pergunta, buscarAlternativasDTO(pergunta));
    }

    private Pergunta buscarEntityPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pergunta nao encontrada"));
    }

    private Pergunta buscarEntityPorIdParaAtualizacao(Long id) {
        return repository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pergunta nao encontrada"));
    }

    private void validarPergunta(PerguntaRequestDTO dto) {
        if (dto.getTipo() != TipoPergunta.MULTIPLA_ESCOLHA) {
            return;
        }

        List<AlternativaPerguntaDTO> alternativas = montarAlternativasDTO(dto);
        if (alternativas.size() < 2) {
            throw new BadRequestException("Perguntas de multipla escolha precisam ter pelo menos duas alternativas");
        }

        Set<String> letras = new HashSet<>();
        Set<String> textos = new HashSet<>();
        long corretas = 0;
        AlternativaPerguntaDTO alternativaCorreta = null;
        for (AlternativaPerguntaDTO alternativa : alternativas) {
            if (alternativa == null) {
                throw new BadRequestException("Alternativa invalida");
            }
            String letra = alternativa.getLetra() == null ? "" : alternativa.getLetra().trim().toUpperCase();
            if (letra.isBlank()) {
                throw new BadRequestException("A letra da alternativa e obrigatoria");
            }
            if (!LETRAS_ALTERNATIVAS_PERMITIDAS.contains(letra)) {
                throw new BadRequestException("A letra da alternativa deve estar entre A e E");
            }
            if (!letras.add(letra)) {
                throw new BadRequestException("As letras das alternativas nao podem se repetir");
            }
            String texto = alternativa.getTexto() == null ? "" : normalizarEnunciado(alternativa.getTexto());
            if (texto.isBlank()) {
                throw new BadRequestException("O texto da alternativa e obrigatorio");
            }
            if (!textos.add(texto)) {
                throw new BadRequestException("Os textos das alternativas nao podem se repetir");
            }
            if (alternativaCorreta(alternativa, dto)) {
                corretas++;
                alternativaCorreta = alternativa;
            }
        }

        if (corretas != 1) {
            throw new BadRequestException("Perguntas de multipla escolha precisam ter exatamente uma alternativa correta");
        }

        if (!gabaritoApontaParaAlternativaCorreta(alternativaCorreta, dto)) {
            throw new BadRequestException("O gabarito deve corresponder a alternativa correta");
        }
    }

    private List<AlternativaPergunta> salvarAlternativas(Pergunta pergunta, PerguntaRequestDTO dto) {
        List<AlternativaPergunta> alternativas = montarAlternativasDTO(dto).stream()
                .map(alternativaDTO -> paraAlternativaEntity(pergunta, alternativaDTO, dto))
                .toList();

        if (alternativas.isEmpty()) {
            return List.of();
        }
        return alternativaRepository.saveAll(alternativas);
    }

    private List<AlternativaPerguntaDTO> buscarAlternativasDTO(Pergunta pergunta) {
        if (pergunta.getId() == null) {
            return List.of();
        }

        List<AlternativaPerguntaDTO> alternativas = alternativaRepository.findByPerguntaIdOrderByLetra(pergunta.getId())
                .stream()
                .map(this::paraAlternativaDTO)
                .toList();
        return alternativas;
    }

    private Map<Long, List<AlternativaPerguntaDTO>> buscarAlternativasPorPergunta(
            List<Pergunta> perguntas) {
        List<Long> idsPerguntas = perguntas.stream()
                .map(Pergunta::getId)
                .filter(id -> id != null)
                .toList();
        if (idsPerguntas.isEmpty()) {
            return Map.of();
        }

        return agruparAlternativas(
                alternativaRepository.findByPerguntaIdInOrderByPerguntaIdAscLetraAsc(idsPerguntas));
    }

    private Map<Long, List<AlternativaPerguntaDTO>> agruparAlternativas(
            List<AlternativaPergunta> alternativas) {
        return alternativas.stream()
                .collect(Collectors.groupingBy(
                        alternativa -> alternativa.getPergunta().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(this::paraAlternativaDTO, Collectors.toList())));
    }

    private PerguntaResponseDTO paraDTO(
            Pergunta pergunta,
            Map<Long, List<AlternativaPerguntaDTO>> alternativasPorPergunta) {
        List<AlternativaPerguntaDTO> alternativas = alternativasPorPergunta
                .getOrDefault(pergunta.getId(), List.of());
        return mapper.toResponse(pergunta, alternativas);
    }

    private PerguntaAlunoDTO paraAlunoDTO(
            Pergunta pergunta,
            Map<Long, List<AlternativaPerguntaDTO>> alternativasPorPergunta) {
        List<AlternativaAlunoDTO> alternativas = alternativasPorPergunta
                .getOrDefault(pergunta.getId(), List.of())
                .stream()
                .map(alternativa -> new AlternativaAlunoDTO(
                        alternativa.getId(),
                        alternativa.getLetra(),
                        alternativa.getTexto()))
                .toList();

        Long idCaso = pergunta.getCasoClinico() != null
                ? pergunta.getCasoClinico().getIdCaso()
                : null;
        return new PerguntaAlunoDTO(
                pergunta.getId(),
                idCaso,
                pergunta.getTexto(),
                alternativas,
                pergunta.getTipo());
    }

    private AlternativaPergunta paraAlternativaEntity(
            Pergunta pergunta,
            AlternativaPerguntaDTO dto,
            PerguntaRequestDTO perguntaDTO) {
        AlternativaPergunta alternativa = new AlternativaPergunta();
        alternativa.setPergunta(pergunta);
        alternativa.setLetra(dto.getLetra().trim().toUpperCase());
        alternativa.setTexto(dto.getTexto());
        alternativa.setCorreta(dto.getCorreta() != null
                ? dto.getCorreta()
                : correspondeGabarito(dto.getLetra(), perguntaDTO));

        return alternativa;
    }

    private boolean alternativaCorreta(AlternativaPerguntaDTO alternativa, PerguntaRequestDTO perguntaDTO) {
        return alternativa.getCorreta() != null
                ? alternativa.getCorreta()
                : correspondeGabarito(alternativa.getLetra(), perguntaDTO);
    }

    private boolean gabaritoApontaParaAlternativaCorreta(
            AlternativaPerguntaDTO alternativa,
            PerguntaRequestDTO perguntaDTO) {
        return corresponde(alternativa.getLetra(), perguntaDTO.getGabarito())
                || corresponde(alternativa.getTexto(), perguntaDTO.getGabarito())
                || corresponde(alternativa.getLetra(), perguntaDTO.getResposta())
                || corresponde(alternativa.getTexto(), perguntaDTO.getResposta());
    }

    private AlternativaPerguntaDTO paraAlternativaDTO(AlternativaPergunta alternativa) {
        return new AlternativaPerguntaDTO(
                alternativa.getId(),
                alternativa.getLetra(),
                alternativa.getTexto(),
                alternativa.getCorreta());
    }

    private List<AlternativaPerguntaDTO> montarAlternativasDTO(PerguntaRequestDTO dto) {
        return dto.getAlternativas() == null ? List.of() : dto.getAlternativas();
    }

    private boolean correspondeGabarito(String letra, PerguntaRequestDTO perguntaDTO) {
        return corresponde(letra, perguntaDTO.getGabarito()) || corresponde(letra, perguntaDTO.getResposta());
    }

    private boolean corresponde(String valor, String referencia) {
        return valor != null && referencia != null && valor.trim().equalsIgnoreCase(referencia.trim());
    }
}
