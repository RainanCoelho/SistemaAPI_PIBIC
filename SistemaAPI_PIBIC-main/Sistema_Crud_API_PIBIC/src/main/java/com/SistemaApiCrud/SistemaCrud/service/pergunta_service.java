package com.SistemaApiCrud.SistemaCrud.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.SistemaApiCrud.SistemaCrud.DTO.alternativa_pergunta_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.pergunta_request_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.pergunta_response_DTO;
import com.SistemaApiCrud.SistemaCrud.entity.AlternativaPergunta;
import com.SistemaApiCrud.SistemaCrud.entity.casos_clinicos;
import com.SistemaApiCrud.SistemaCrud.entity.enums.TipoPergunta;
import com.SistemaApiCrud.SistemaCrud.entity.pergunta;
import com.SistemaApiCrud.SistemaCrud.exception.BadRequestException;
import com.SistemaApiCrud.SistemaCrud.exception.RecursoNaoEncontradoException;
import com.SistemaApiCrud.SistemaCrud.mapper.PerguntaMapper;
import com.SistemaApiCrud.SistemaCrud.repository.alternativa_pergunta_repository;
import com.SistemaApiCrud.SistemaCrud.repository.caso_clinico_repository;
import com.SistemaApiCrud.SistemaCrud.repository.pergunta_repository;

@Service
public class pergunta_service {

    private final pergunta_repository repository;
    private final caso_clinico_repository casoRepository;
    private final alternativa_pergunta_repository alternativaRepository;
    private final PerguntaMapper mapper;

    public pergunta_service(
            pergunta_repository repository,
            caso_clinico_repository casoRepository,
            alternativa_pergunta_repository alternativaRepository,
            PerguntaMapper mapper) {
        this.repository = repository;
        this.casoRepository = casoRepository;
        this.alternativaRepository = alternativaRepository;
        this.mapper = mapper;
    }

    public Page<pergunta_response_DTO> listar(Pageable pageable) {
        return repository.findAll(pageable).map(this::paraDTO);
    }

    public Page<pergunta_response_DTO> listarPorProfessor(Long idProfessor, Pageable pageable) {
        return repository.findByCasoClinicoProfessorId(idProfessor, pageable).map(this::paraDTO);
    }

    public pergunta_response_DTO buscarPorId(Long id) {
        return paraDTO(buscarEntityPorId(id));
    }

    public Page<pergunta_response_DTO> listarPorCaso(Long idCaso, Pageable pageable) {
        if (!casoRepository.existsById(idCaso)) {
            throw new RecursoNaoEncontradoException("Caso clinico nao encontrado");
        }

        return repository.findByCasoClinicoIdCaso(idCaso, pageable).map(this::paraDTO);
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
        casos_clinicos caso = buscarCaso(idCaso);
        pergunta pergunta = mapper.toEntity(dto, caso);
        pergunta perguntaSalva = repository.save(pergunta);
        salvarAlternativas(perguntaSalva, dto);
        return paraDTO(perguntaSalva);
    }

    @Transactional
    public pergunta_response_DTO atualizar(Long id, pergunta_request_DTO dto) {
        pergunta pergunta = buscarEntityPorId(id);
        validarPergunta(dto);

        casos_clinicos caso = dto.getIdCaso() != null ? buscarCaso(dto.getIdCaso()) : null;
        mapper.updateEntity(dto, pergunta, caso);

        pergunta perguntaAtualizada = repository.save(pergunta);
        alternativaRepository.deleteByPerguntaId(id);
        alternativaRepository.flush();
        salvarAlternativas(perguntaAtualizada, dto);
        return paraDTO(perguntaAtualizada);
    }

    @Transactional
    public void deletar(Long id) {
        buscarEntityPorId(id);
        alternativaRepository.deleteByPerguntaId(id);
        repository.deleteById(id);
    }

    private pergunta_response_DTO paraDTO(pergunta pergunta) {
        return mapper.toResponse(pergunta, buscarAlternativasDTO(pergunta));
    }

    private casos_clinicos buscarCaso(Long idCaso) {
        return casoRepository.findById(idCaso)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Caso clinico nao encontrado"));
    }

    private pergunta buscarEntityPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pergunta nao encontrada"));
    }

    private void validarPergunta(pergunta_request_DTO dto) {
        if (dto.getTipo() == TipoPergunta.MULTIPLA_ESCOLHA && montarAlternativasDTO(dto).isEmpty()) {
            throw new BadRequestException("Perguntas de multipla escolha precisam ter alternativas");
        }
    }

    private void salvarAlternativas(pergunta pergunta, pergunta_request_DTO dto) {
        List<AlternativaPergunta> alternativas = montarAlternativasDTO(dto).stream()
                .map(alternativaDTO -> paraAlternativaEntity(pergunta, alternativaDTO, dto))
                .toList();

        if (!alternativas.isEmpty()) {
            alternativaRepository.saveAll(alternativas);
        }
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
