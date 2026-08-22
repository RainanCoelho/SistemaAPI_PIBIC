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

import com.SistemaApiCrud.SistemaCrud.DTO.alternativa_pergunta_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.alternativa_aluno_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.pergunta_aluno_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.pergunta_request_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.pergunta_response_DTO;
import com.SistemaApiCrud.SistemaCrud.entity.AlternativaPergunta;
import com.SistemaApiCrud.SistemaCrud.entity.casos_clinicos;
import com.SistemaApiCrud.SistemaCrud.entity.enums.TipoPergunta;
import com.SistemaApiCrud.SistemaCrud.entity.pergunta;
import com.SistemaApiCrud.SistemaCrud.exception.BadRequestException;
import com.SistemaApiCrud.SistemaCrud.exception.ConflitoEstadoException;
import com.SistemaApiCrud.SistemaCrud.exception.RecursoNaoEncontradoException;
import com.SistemaApiCrud.SistemaCrud.mapper.PerguntaMapper;
import com.SistemaApiCrud.SistemaCrud.repository.alternativa_pergunta_repository;
import com.SistemaApiCrud.SistemaCrud.repository.caso_clinico_repository;
import com.SistemaApiCrud.SistemaCrud.repository.conteudo_clinico_repository;
import com.SistemaApiCrud.SistemaCrud.repository.paciente_repository;
import com.SistemaApiCrud.SistemaCrud.repository.pergunta_repository;

@Service
public class pergunta_service {

    private static final Set<String> LETRAS_ALTERNATIVAS_PERMITIDAS =
            Set.of("A", "B", "C", "D", "E");

    private final pergunta_repository repository;
    private final caso_clinico_repository casoRepository;
    private final alternativa_pergunta_repository alternativaRepository;
    private final conteudo_clinico_repository conteudoRepository;
    private final paciente_repository pacienteRepository;
    private final PerguntaMapper mapper;
    private final CasoClinicoLockService casoLockService;

    public pergunta_service(
            pergunta_repository repository,
            caso_clinico_repository casoRepository,
            alternativa_pergunta_repository alternativaRepository,
            conteudo_clinico_repository conteudoRepository,
            paciente_repository pacienteRepository,
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

    public Page<pergunta_response_DTO> listar(Pageable pageable) {
        Page<pergunta> perguntas = repository.findAll(pageable);
        Map<Long, List<alternativa_pergunta_DTO>> alternativas = buscarAlternativasPorPergunta(
                perguntas.getContent());
        return perguntas.map(pergunta -> paraDTO(pergunta, alternativas));
    }

    public Page<pergunta_response_DTO> listarPorProfessor(Long idProfessor, Pageable pageable) {
        Page<pergunta> perguntas = repository.findByCasoClinicoProfessorId(idProfessor, pageable);
        Map<Long, List<alternativa_pergunta_DTO>> alternativas = buscarAlternativasPorPergunta(
                perguntas.getContent());
        return perguntas.map(pergunta -> paraDTO(pergunta, alternativas));
    }

    public pergunta_response_DTO buscarPorId(Long id) {
        return paraDTO(buscarEntityPorId(id));
    }

    public Page<pergunta_response_DTO> listarPorCaso(Long idCaso, Pageable pageable) {
        if (!casoRepository.existsById(idCaso)) {
            throw new RecursoNaoEncontradoException("Caso clinico nao encontrado");
        }

        Page<pergunta> perguntas = repository.findByCasoClinicoIdCaso(idCaso, pageable);
        Map<Long, List<alternativa_pergunta_DTO>> alternativas = buscarAlternativasPorPergunta(
                perguntas.getContent());
        return perguntas.map(pergunta -> paraDTO(pergunta, alternativas));
    }

    public List<pergunta_response_DTO> listarTodasPorCaso(Long idCaso) {
        List<pergunta> perguntas = repository.findByCasoClinicoIdCaso(idCaso);
        Map<Long, List<alternativa_pergunta_DTO>> alternativas = buscarAlternativasPorPergunta(perguntas);
        return perguntas.stream()
                .map(pergunta -> paraDTO(pergunta, alternativas))
                .toList();
    }

    public List<pergunta_aluno_DTO> listarTodasParaAlunoPorCaso(Long idCaso) {
        List<pergunta> perguntas = repository.findByCasoClinicoIdCaso(idCaso);
        Map<Long, List<alternativa_pergunta_DTO>> alternativas = buscarAlternativasPorPergunta(perguntas);
        return perguntas.stream()
                .map(pergunta -> paraAlunoDTO(pergunta, alternativas))
                .toList();
    }

    @Transactional
    public pergunta_response_DTO salvar(pergunta_request_DTO dto) {
        if (dto.getIdCaso() == null) {
            throw new BadRequestException("O caso clinico e obrigatorio");
        }

        return salvarNoCaso(dto.getIdCaso(), dto);
    }

    @Transactional
    public pergunta_response_DTO salvarEmCaso(Long idCaso, pergunta_request_DTO dto) {
        return salvarNoCaso(idCaso, dto);
    }

    private pergunta_response_DTO salvarNoCaso(Long idCaso, pergunta_request_DTO dto) {
        validarPergunta(dto);
        casos_clinicos caso = casoLockService.bloquearRascunho(idCaso);
        pergunta pergunta = mapper.toEntity(dto, caso);
        pergunta perguntaSalva = repository.save(pergunta);
        List<AlternativaPergunta> alternativas = salvarAlternativas(perguntaSalva, dto);
        return mapper.toResponse(
                perguntaSalva,
                alternativas.stream().map(this::paraAlternativaDTO).toList());
    }

    @Transactional
    public List<pergunta_response_DTO> salvarLoteEmCaso(
            Long idCaso,
            List<pergunta_request_DTO> perguntasDTO) {
        return salvarLoteInterno(idCaso, perguntasDTO, null, null);
    }

    @Transactional
    public List<pergunta_response_DTO> salvarLoteEmCaso(
            Long idCaso,
            List<pergunta_request_DTO> perguntasDTO,
            String fingerprintEsperado) {
        return salvarLoteInterno(idCaso, perguntasDTO, fingerprintEsperado, null);
    }

    @Transactional
    public List<pergunta_response_DTO> salvarLoteEmCaso(
            Long idCaso,
            List<pergunta_request_DTO> perguntasDTO,
            String fingerprintEsperado,
            long quantidadePerguntasEsperada) {
        return salvarLoteInterno(
                idCaso,
                perguntasDTO,
                fingerprintEsperado,
                quantidadePerguntasEsperada);
    }

    private List<pergunta_response_DTO> salvarLoteInterno(
            Long idCaso,
            List<pergunta_request_DTO> perguntasDTO,
            String fingerprintEsperado,
            Long quantidadePerguntasEsperada) {
        if (perguntasDTO == null || perguntasDTO.isEmpty()) {
            throw new BadRequestException("Informe ao menos uma pergunta");
        }

        perguntasDTO.forEach(this::validarPergunta);
        casos_clinicos caso = casoLockService.bloquearRascunho(idCaso);
        validarContextoInalterado(
                caso,
                fingerprintEsperado,
                quantidadePerguntasEsperada);
        validarEnunciadosGerados(
                caso.getIdCaso(),
                perguntasDTO,
                quantidadePerguntasEsperada);
        List<pergunta> perguntas = perguntasDTO.stream()
                .map(dto -> mapper.toEntity(dto, caso))
                .toList();
        List<pergunta> perguntasSalvas = repository.saveAll(perguntas);

        List<AlternativaPergunta> alternativas = new ArrayList<>();
        for (int indice = 0; indice < perguntasSalvas.size(); indice++) {
            pergunta perguntaSalva = perguntasSalvas.get(indice);
            pergunta_request_DTO dto = perguntasDTO.get(indice);
            alternativas.addAll(montarAlternativasDTO(dto).stream()
                    .map(alternativa -> paraAlternativaEntity(perguntaSalva, alternativa, dto))
                    .toList());
        }

        List<AlternativaPergunta> alternativasSalvas = alternativas.isEmpty()
                ? List.of()
                : alternativaRepository.saveAll(alternativas);
        Map<Long, List<alternativa_pergunta_DTO>> alternativasPorPergunta =
                agruparAlternativas(alternativasSalvas);

        return perguntasSalvas.stream()
                .map(pergunta -> paraDTO(pergunta, alternativasPorPergunta))
                .toList();
    }

    private void validarContextoInalterado(
            casos_clinicos caso,
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
            List<pergunta_request_DTO> perguntasDTO,
            Long quantidadePerguntasEsperada) {
        if (quantidadePerguntasEsperada == null) {
            return;
        }

        Set<String> enunciados = repository.findTextosByCasoClinicoIdCaso(idCaso)
                .stream()
                .map(this::normalizarEnunciado)
                .collect(Collectors.toSet());
        for (pergunta_request_DTO pergunta : perguntasDTO) {
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
    public pergunta_response_DTO atualizar(Long id, pergunta_request_DTO dto) {
        pergunta pergunta = buscarEntityPorIdParaAtualizacao(id);
        validarPergunta(dto);
        Long idCasoAtual = pergunta.getCasoClinico().getIdCaso();
        Long idCasoDestino = dto.getIdCaso() != null ? dto.getIdCaso() : idCasoAtual;
        Map<Long, casos_clinicos> casosBloqueados = casoLockService.bloquearRascunhos(
                List.of(idCasoAtual, idCasoDestino));
        casos_clinicos caso = dto.getIdCaso() != null
                ? casosBloqueados.get(idCasoDestino)
                : null;
        mapper.updateEntity(dto, pergunta, caso);

        pergunta perguntaAtualizada = repository.save(pergunta);
        alternativaRepository.deleteByPerguntaId(id);
        alternativaRepository.flush();
        salvarAlternativas(perguntaAtualizada, dto);
        return paraDTO(perguntaAtualizada);
    }

    @Transactional
    public void deletar(Long id) {
        pergunta pergunta = buscarEntityPorIdParaAtualizacao(id);
        casoLockService.bloquearRascunho(pergunta.getCasoClinico().getIdCaso());
        alternativaRepository.deleteByPerguntaId(id);
        repository.deleteById(id);
    }

    private pergunta_response_DTO paraDTO(pergunta pergunta) {
        return mapper.toResponse(pergunta, buscarAlternativasDTO(pergunta));
    }

    private pergunta buscarEntityPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pergunta nao encontrada"));
    }

    private pergunta buscarEntityPorIdParaAtualizacao(Long id) {
        return repository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pergunta nao encontrada"));
    }

    private void validarPergunta(pergunta_request_DTO dto) {
        if (dto.getTipo() != TipoPergunta.MULTIPLA_ESCOLHA) {
            return;
        }

        List<alternativa_pergunta_DTO> alternativas = montarAlternativasDTO(dto);
        if (alternativas.size() < 2) {
            throw new BadRequestException("Perguntas de multipla escolha precisam ter pelo menos duas alternativas");
        }

        Set<String> letras = new HashSet<>();
        Set<String> textos = new HashSet<>();
        long corretas = 0;
        alternativa_pergunta_DTO alternativaCorreta = null;
        for (alternativa_pergunta_DTO alternativa : alternativas) {
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

    private List<AlternativaPergunta> salvarAlternativas(pergunta pergunta, pergunta_request_DTO dto) {
        List<AlternativaPergunta> alternativas = montarAlternativasDTO(dto).stream()
                .map(alternativaDTO -> paraAlternativaEntity(pergunta, alternativaDTO, dto))
                .toList();

        if (alternativas.isEmpty()) {
            return List.of();
        }
        return alternativaRepository.saveAll(alternativas);
    }

    private List<alternativa_pergunta_DTO> buscarAlternativasDTO(pergunta pergunta) {
        if (pergunta.getId() == null) {
            return List.of();
        }

        List<alternativa_pergunta_DTO> alternativas = alternativaRepository.findByPerguntaIdOrderByLetra(pergunta.getId())
                .stream()
                .map(this::paraAlternativaDTO)
                .toList();

        if (!alternativas.isEmpty()) {
            return alternativas;
        }

        pergunta_request_DTO dto = new pergunta_request_DTO();
        dto.setAlternativaA(pergunta.getAlternativaA());
        dto.setAlternativaB(pergunta.getAlternativaB());
        dto.setAlternativaC(pergunta.getAlternativaC());
        dto.setAlternativaD(pergunta.getAlternativaD());
        dto.setAlternativaE(pergunta.getAlternativaE());
        dto.setGabarito(pergunta.getGabarito());
        dto.setResposta(pergunta.getResposta());

        return montarAlternativasLegadas(dto);
    }

    private Map<Long, List<alternativa_pergunta_DTO>> buscarAlternativasPorPergunta(
            List<pergunta> perguntas) {
        List<Long> idsPerguntas = perguntas.stream()
                .map(pergunta::getId)
                .filter(id -> id != null)
                .toList();
        if (idsPerguntas.isEmpty()) {
            return Map.of();
        }

        return agruparAlternativas(
                alternativaRepository.findByPerguntaIdInOrderByPerguntaIdAscLetraAsc(idsPerguntas));
    }

    private Map<Long, List<alternativa_pergunta_DTO>> agruparAlternativas(
            List<AlternativaPergunta> alternativas) {
        return alternativas.stream()
                .collect(Collectors.groupingBy(
                        alternativa -> alternativa.getPergunta().getId(),
                        LinkedHashMap::new,
                        Collectors.mapping(this::paraAlternativaDTO, Collectors.toList())));
    }

    private pergunta_response_DTO paraDTO(
            pergunta pergunta,
            Map<Long, List<alternativa_pergunta_DTO>> alternativasPorPergunta) {
        List<alternativa_pergunta_DTO> alternativas = alternativasPorPergunta
                .getOrDefault(pergunta.getId(), List.of());
        if (alternativas.isEmpty()) {
            pergunta_request_DTO dto = new pergunta_request_DTO();
            dto.setAlternativaA(pergunta.getAlternativaA());
            dto.setAlternativaB(pergunta.getAlternativaB());
            dto.setAlternativaC(pergunta.getAlternativaC());
            dto.setAlternativaD(pergunta.getAlternativaD());
            dto.setAlternativaE(pergunta.getAlternativaE());
            dto.setGabarito(pergunta.getGabarito());
            dto.setResposta(pergunta.getResposta());
            alternativas = montarAlternativasLegadas(dto);
        }
        return mapper.toResponse(pergunta, alternativas);
    }

    private pergunta_aluno_DTO paraAlunoDTO(
            pergunta pergunta,
            Map<Long, List<alternativa_pergunta_DTO>> alternativasPorPergunta) {
        List<alternativa_aluno_DTO> alternativas = alternativasPorPergunta
                .getOrDefault(pergunta.getId(), List.of())
                .stream()
                .map(alternativa -> new alternativa_aluno_DTO(
                        alternativa.getId(),
                        alternativa.getLetra(),
                        alternativa.getTexto()))
                .toList();

        if (alternativas.isEmpty()) {
            pergunta_request_DTO dto = new pergunta_request_DTO();
            dto.setAlternativaA(pergunta.getAlternativaA());
            dto.setAlternativaB(pergunta.getAlternativaB());
            dto.setAlternativaC(pergunta.getAlternativaC());
            dto.setAlternativaD(pergunta.getAlternativaD());
            dto.setAlternativaE(pergunta.getAlternativaE());
            dto.setGabarito(pergunta.getGabarito());
            dto.setResposta(pergunta.getResposta());
            alternativas = montarAlternativasLegadas(dto).stream()
                    .map(alternativa -> new alternativa_aluno_DTO(
                            alternativa.getId(),
                            alternativa.getLetra(),
                            alternativa.getTexto()))
                    .toList();
        }

        Long idCaso = pergunta.getCasoClinico() != null
                ? pergunta.getCasoClinico().getIdCaso()
                : null;
        return new pergunta_aluno_DTO(
                pergunta.getId(),
                idCaso,
                pergunta.getTexto(),
                pergunta.getAlternativaA(),
                pergunta.getAlternativaB(),
                pergunta.getAlternativaC(),
                pergunta.getAlternativaD(),
                pergunta.getAlternativaE(),
                alternativas,
                pergunta.getTipo());
    }

    private AlternativaPergunta paraAlternativaEntity(
            pergunta pergunta,
            alternativa_pergunta_DTO dto,
            pergunta_request_DTO perguntaDTO) {
        AlternativaPergunta alternativa = new AlternativaPergunta();
        alternativa.setPergunta(pergunta);
        alternativa.setLetra(dto.getLetra().trim().toUpperCase());
        alternativa.setTexto(dto.getTexto());
        alternativa.setCorreta(dto.getCorreta() != null
                ? dto.getCorreta()
                : correspondeGabarito(dto.getLetra(), perguntaDTO));

        return alternativa;
    }

    private boolean alternativaCorreta(alternativa_pergunta_DTO alternativa, pergunta_request_DTO perguntaDTO) {
        return alternativa.getCorreta() != null
                ? alternativa.getCorreta()
                : correspondeGabarito(alternativa.getLetra(), perguntaDTO);
    }

    private boolean gabaritoApontaParaAlternativaCorreta(
            alternativa_pergunta_DTO alternativa,
            pergunta_request_DTO perguntaDTO) {
        return corresponde(alternativa.getLetra(), perguntaDTO.getGabarito())
                || corresponde(alternativa.getTexto(), perguntaDTO.getGabarito())
                || corresponde(alternativa.getLetra(), perguntaDTO.getResposta())
                || corresponde(alternativa.getTexto(), perguntaDTO.getResposta());
    }

    private alternativa_pergunta_DTO paraAlternativaDTO(AlternativaPergunta alternativa) {
        return new alternativa_pergunta_DTO(
                alternativa.getId(),
                alternativa.getLetra(),
                alternativa.getTexto(),
                alternativa.getCorreta());
    }

    private List<alternativa_pergunta_DTO> montarAlternativasDTO(pergunta_request_DTO dto) {
        if (dto.getAlternativas() != null && !dto.getAlternativas().isEmpty()) {
            return dto.getAlternativas();
        }

        return montarAlternativasLegadas(dto);
    }

    private List<alternativa_pergunta_DTO> montarAlternativasLegadas(pergunta_request_DTO dto) {
        List<alternativa_pergunta_DTO> alternativas = new ArrayList<>();

        adicionarAlternativaLegada(alternativas, "A", dto.getAlternativaA(), dto);
        adicionarAlternativaLegada(alternativas, "B", dto.getAlternativaB(), dto);
        adicionarAlternativaLegada(alternativas, "C", dto.getAlternativaC(), dto);
        adicionarAlternativaLegada(alternativas, "D", dto.getAlternativaD(), dto);
        adicionarAlternativaLegada(alternativas, "E", dto.getAlternativaE(), dto);

        return alternativas;
    }

    private void adicionarAlternativaLegada(
            List<alternativa_pergunta_DTO> alternativas,
            String letra,
            String texto,
            pergunta_request_DTO perguntaDTO) {
        if (texto == null || texto.isBlank()) {
            return;
        }

        alternativas.add(new alternativa_pergunta_DTO(null, letra, texto, correspondeGabarito(letra, perguntaDTO)));
    }

    private boolean correspondeGabarito(String letra, pergunta_request_DTO perguntaDTO) {
        return corresponde(letra, perguntaDTO.getGabarito()) || corresponde(letra, perguntaDTO.getResposta());
    }

    private boolean corresponde(String valor, String referencia) {
        return valor != null && referencia != null && valor.trim().equalsIgnoreCase(referencia.trim());
    }
}
