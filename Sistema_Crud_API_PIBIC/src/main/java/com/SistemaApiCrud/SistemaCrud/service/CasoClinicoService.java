package com.SistemaApiCrud.SistemaCrud.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.SistemaApiCrud.SistemaCrud.dto.CasoClinicoAlunoDTO;
import com.SistemaApiCrud.SistemaCrud.dto.CasoClinicoCompletoDTO;
import com.SistemaApiCrud.SistemaCrud.dto.CasoClinicoRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.CasoClinicoResponseDTO;
import com.SistemaApiCrud.SistemaCrud.dto.ConteudoClinicoDTO;
import com.SistemaApiCrud.SistemaCrud.dto.ConteudoClinicoAlunoDTO;
import com.SistemaApiCrud.SistemaCrud.dto.PacienteDTO;
import com.SistemaApiCrud.SistemaCrud.dto.PerguntaAlunoDTO;
import com.SistemaApiCrud.SistemaCrud.dto.PerguntaResponseDTO;
import com.SistemaApiCrud.SistemaCrud.entity.Professor;
import com.SistemaApiCrud.SistemaCrud.entity.TentativaCaso;
import com.SistemaApiCrud.SistemaCrud.entity.CasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.ConteudoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.enums.StatusCasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.Paciente;
import com.SistemaApiCrud.SistemaCrud.exception.BadRequestException;
import com.SistemaApiCrud.SistemaCrud.exception.BusinessException;
import com.SistemaApiCrud.SistemaCrud.exception.RecursoNaoEncontradoException;
import com.SistemaApiCrud.SistemaCrud.mapper.CasoClinicoMapper;
import com.SistemaApiCrud.SistemaCrud.repository.CasoClinicoRepository;
import com.SistemaApiCrud.SistemaCrud.repository.ConteudoClinicoRepository;
import com.SistemaApiCrud.SistemaCrud.repository.PacienteRepository;
import com.SistemaApiCrud.SistemaCrud.repository.PerguntaRepository;
import com.SistemaApiCrud.SistemaCrud.repository.ProfessorRepository;

import jakarta.persistence.criteria.Predicate;

@Service
public class CasoClinicoService {

    private final CasoClinicoRepository repository;
    private final ProfessorRepository professorRepository;
    private final PacienteRepository pacienteRepository;
    private final ConteudoClinicoRepository conteudoRepository;
    private final PerguntaRepository perguntaRepository;
    private final CasoClinicoMapper mapper;
    private final PerguntaService perguntaService;
    private final TentativaCasoService tentativaCasoService;
    private final AutorizacaoUsuarioService autorizacaoService;

    public CasoClinicoService(
            CasoClinicoRepository repository,
            ProfessorRepository professorRepository,
            PacienteRepository pacienteRepository,
            ConteudoClinicoRepository conteudoRepository,
            PerguntaRepository perguntaRepository,
            CasoClinicoMapper mapper,
            PerguntaService perguntaService,
            TentativaCasoService tentativaCasoService,
            AutorizacaoUsuarioService autorizacaoService) {
        this.repository = repository;
        this.professorRepository = professorRepository;
        this.pacienteRepository = pacienteRepository;
        this.conteudoRepository = conteudoRepository;
        this.perguntaRepository = perguntaRepository;
        this.mapper = mapper;
        this.perguntaService = perguntaService;
        this.tentativaCasoService = tentativaCasoService;
        this.autorizacaoService = autorizacaoService;
    }

    public Page<CasoClinicoResponseDTO> listarPaginado(
            StatusCasoClinico status,
            Long idProfessor,
            String termo,
            Pageable pageable) {
        if (idProfessor != null && !professorRepository.existsById(idProfessor)) {
            throw new RecursoNaoEncontradoException("Professor nao encontrado");
        }

        return repository.findAll(filtrarCasos(status, idProfessor, termo), pageable)
                .map(mapper::toResponse);
    }

    public Page<CasoClinicoResponseDTO> listarPublicados(Pageable pageable) {
        return repository.findByStatus(StatusCasoClinico.PUBLICADO, pageable).map(mapper::toResponse);
    }

    public Page<CasoClinicoResponseDTO> listarPorProfessor(Long idProfessor, Pageable pageable) {
        if (!professorRepository.existsById(idProfessor)) {
            throw new RecursoNaoEncontradoException("Professor nao encontrado");
        }

        return repository.findByProfessorId(idProfessor, pageable).map(mapper::toResponse);
    }

    public CasoClinicoResponseDTO buscarPorId(Long id) {
        return mapper.toResponse(buscarEntityPorId(id));
    }

    public CasoClinicoCompletoDTO buscarCompletoPorId(Long id) {
        CasoClinico caso = buscarEntityPorId(id);
        return montarCompleto(caso);
    }

    @Transactional
    public CasoClinicoAlunoDTO buscarCompletoPublicadoPorId(Long id, Long idAluno) {
        CasoClinico caso = buscarEntityPorId(id);
        if (caso.getStatus() != StatusCasoClinico.PUBLICADO) {
            throw new BusinessException("O caso clinico ainda nao esta publicado");
        }

        TentativaCaso tentativa = tentativaCasoService.iniciarOuBuscar(idAluno, caso);
        return montarCompletoParaAluno(caso, tentativa);
    }

    public CasoClinicoResponseDTO salvar(CasoClinicoRequestDTO dto, Long idProfessorAutorizado) {
        Professor professor = buscarProfessorObrigatorio(idProfessorAutorizado);
        CasoClinico caso = mapper.toEntity(dto, professor);
        return mapper.toResponse(repository.save(caso));
    }

    @Transactional
    public CasoClinicoResponseDTO atualizar(Long id, CasoClinicoRequestDTO dto, Long idProfessorAutorizado) {
        CasoClinico caso = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Caso clinico nao encontrado"));
        autorizacaoService.validarAcessoCaso(caso);
        CasoClinicoPolicy.validarRascunho(caso);
        Professor professor = idProfessorAutorizado != null ? buscarProfessor(idProfessorAutorizado) : null;
        mapper.updateEntity(dto, caso, professor);
        return mapper.toResponse(repository.save(caso));
    }

    @Transactional
    public CasoClinicoResponseDTO publicar(Long id) {
        CasoClinico caso = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Caso clinico nao encontrado"));
        autorizacaoService.validarAcessoCaso(caso);
        CasoClinicoPolicy.validarRascunho(caso);
        validarCasoPublicavel(caso);
        caso.setStatus(StatusCasoClinico.PUBLICADO);
        return mapper.toResponse(repository.save(caso));
    }

    @Transactional
    public CasoClinicoResponseDTO arquivar(Long id) {
        CasoClinico caso = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Caso clinico nao encontrado"));
        autorizacaoService.validarAcessoCaso(caso);
        CasoClinicoPolicy.validarArquivavel(caso);
        if (caso.getStatus() == StatusCasoClinico.ARQUIVADO) {
            return mapper.toResponse(caso);
        }
        caso.setStatus(StatusCasoClinico.ARQUIVADO);
        return mapper.toResponse(repository.save(caso));
    }

    @Transactional
    public void deletar(Long id) {
        CasoClinico caso = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Caso clinico nao encontrado"));
        autorizacaoService.validarAcessoCaso(caso);
        CasoClinicoPolicy.validarRascunho(caso);
        repository.deleteById(id);
    }

    private CasoClinicoCompletoDTO montarCompleto(CasoClinico caso) {
        Long id = caso.getIdCaso();

        List<PacienteDTO> pacientes = pacienteRepository.findByCasoClinicoIdCaso(id)
                .stream()
                .map(this::paraPacienteDTO)
                .toList();

        List<ConteudoClinicoDTO> conteudos = conteudoRepository.findByCasoClinicoIdCaso(id)
                .stream()
                .map(this::paraConteudoDTO)
                .toList();

        List<PerguntaResponseDTO> perguntas = perguntaService.listarTodasPorCaso(id);

        return new CasoClinicoCompletoDTO(mapper.toResponse(caso), pacientes, conteudos, perguntas);
    }

    private CasoClinicoAlunoDTO montarCompletoParaAluno(
            CasoClinico caso,
            TentativaCaso tentativa) {
        Long id = caso.getIdCaso();

        List<PacienteDTO> pacientes = pacienteRepository.findByCasoClinicoIdCaso(id)
                .stream()
                .map(this::paraPacienteDTO)
                .toList();

        List<ConteudoClinicoAlunoDTO> conteudos = conteudoRepository.findByCasoClinicoIdCaso(id)
                .stream()
                .map(this::paraConteudoAlunoDTO)
                .toList();

        List<PerguntaAlunoDTO> perguntas = perguntaService.listarTodasParaAlunoPorCaso(id);

        return new CasoClinicoAlunoDTO(
                mapper.toResponse(caso),
                pacientes,
                conteudos,
                perguntas,
                tentativa.getDataInicio(),
                tentativa.getDataLimite(),
                tentativaCasoService.calcularSegundosRestantes(tentativa));
    }

    private Professor buscarProfessorObrigatorio(Long idProfessor) {
        if (idProfessor == null) {
            throw new BadRequestException("O professor e obrigatorio");
        }

        return buscarProfessor(idProfessor);
    }

    private Professor buscarProfessor(Long idProfessor) {
        return professorRepository.findById(idProfessor)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Professor nao encontrado"));
    }

    private PacienteDTO paraPacienteDTO(Paciente paciente) {
        PacienteDTO dto = new PacienteDTO();
        dto.setIdPaciente(paciente.getIdPaciente());
        if (paciente.getCasoClinico() != null) {
            dto.setIdCaso(paciente.getCasoClinico().getIdCaso());
        }
        dto.setNome(paciente.getNome());
        dto.setProfissao(paciente.getProfissao());
        dto.setSexo(paciente.getSexo());
        dto.setIdade(paciente.getIdade());
        dto.setEstadoCivil(paciente.getEstadoCivil());
        dto.setAltura(paciente.getAltura());
        dto.setPeso(paciente.getPeso());
        return dto;
    }

    private ConteudoClinicoDTO paraConteudoDTO(ConteudoClinico conteudo) {
        ConteudoClinicoDTO dto = new ConteudoClinicoDTO();
        dto.setIdConteudo(conteudo.getIdConteudo());
        if (conteudo.getCasoClinico() != null) {
            dto.setIdCaso(conteudo.getCasoClinico().getIdCaso());
        }
        dto.setSintomas(conteudo.getSintomas());
        dto.setContexto(conteudo.getContexto());
        dto.setExamClinico(conteudo.getExamClinico());
        dto.setAntecClinico(conteudo.getAntecClinico());
        dto.setDiagEsperado(conteudo.getDiagEsperado());
        return dto;
    }

    private ConteudoClinicoAlunoDTO paraConteudoAlunoDTO(ConteudoClinico conteudo) {
        Long idCaso = conteudo.getCasoClinico() != null ? conteudo.getCasoClinico().getIdCaso() : null;
        return new ConteudoClinicoAlunoDTO(
                conteudo.getIdConteudo(),
                idCaso,
                conteudo.getSintomas(),
                conteudo.getContexto(),
                conteudo.getExamClinico(),
                conteudo.getAntecClinico());
    }

    private Specification<CasoClinico> filtrarCasos(
            StatusCasoClinico status,
            Long idProfessor,
            String termo) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (idProfessor != null) {
                predicates.add(criteriaBuilder.equal(root.get("professor").get("id"), idProfessor));
            }

            if (termo != null && !termo.isBlank()) {
                String termoNormalizado = "%" + termo.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("titulo")), termoNormalizado),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("disciplina")), termoNormalizado),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("areaSaude")), termoNormalizado),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("especialidade")), termoNormalizado)));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void validarCasoPublicavel(CasoClinico caso) {
        Long idCaso = caso.getIdCaso();
        if (!pacienteRepository.existsByCasoClinicoIdCaso(idCaso)) {
            throw new BusinessException("Cadastre ao menos um paciente antes de publicar o caso clinico");
        }

        if (!conteudoRepository.existsByCasoClinicoIdCaso(idCaso)) {
            throw new BusinessException("Cadastre ou gere o conteudo clinico antes de publicar o caso clinico");
        }

        if (!perguntaRepository.existsByCasoClinicoIdCaso(idCaso)) {
            throw new BusinessException("Cadastre ao menos uma pergunta antes de publicar o caso clinico");
        }
    }

    private CasoClinico buscarEntityPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Caso clinico nao encontrado"));
    }
}
