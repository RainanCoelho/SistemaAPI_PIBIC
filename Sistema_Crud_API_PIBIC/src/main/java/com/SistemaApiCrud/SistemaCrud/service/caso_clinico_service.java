package com.SistemaApiCrud.SistemaCrud.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.SistemaApiCrud.SistemaCrud.DTO.alternativa_pergunta_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.alternativa_aluno_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.caso_clinico_aluno_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.caso_clinico_completo_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.caso_clinico_request_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.caso_clinico_response_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.conteudo_clinico_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.conteudo_clinico_aluno_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.paciente_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.pergunta_aluno_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.pergunta_response_DTO;
import com.SistemaApiCrud.SistemaCrud.entity.AlternativaPergunta;
import com.SistemaApiCrud.SistemaCrud.entity.Professor;
import com.SistemaApiCrud.SistemaCrud.entity.TentativaCaso;
import com.SistemaApiCrud.SistemaCrud.entity.casos_clinicos;
import com.SistemaApiCrud.SistemaCrud.entity.conteudo_clinico;
import com.SistemaApiCrud.SistemaCrud.entity.enums.StatusCasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.paciente;
import com.SistemaApiCrud.SistemaCrud.entity.pergunta;
import com.SistemaApiCrud.SistemaCrud.exception.BadRequestException;
import com.SistemaApiCrud.SistemaCrud.exception.BusinessException;
import com.SistemaApiCrud.SistemaCrud.exception.RecursoNaoEncontradoException;
import com.SistemaApiCrud.SistemaCrud.mapper.CasoClinicoMapper;
import com.SistemaApiCrud.SistemaCrud.mapper.PerguntaMapper;
import com.SistemaApiCrud.SistemaCrud.repository.alternativa_pergunta_repository;
import com.SistemaApiCrud.SistemaCrud.repository.caso_clinico_repository;
import com.SistemaApiCrud.SistemaCrud.repository.conteudo_clinico_repository;
import com.SistemaApiCrud.SistemaCrud.repository.paciente_repository;
import com.SistemaApiCrud.SistemaCrud.repository.pergunta_repository;
import com.SistemaApiCrud.SistemaCrud.repository.professor_repository;

import jakarta.persistence.criteria.Predicate;

@Service
public class caso_clinico_service {

    private final caso_clinico_repository repository;
    private final professor_repository professorRepository;
    private final paciente_repository pacienteRepository;
    private final conteudo_clinico_repository conteudoRepository;
    private final pergunta_repository perguntaRepository;
    private final alternativa_pergunta_repository alternativaRepository;
    private final CasoClinicoMapper mapper;
    private final PerguntaMapper perguntaMapper;
    private final TentativaCasoService tentativaCasoService;

    public caso_clinico_service(
            caso_clinico_repository repository,
            professor_repository professorRepository,
            paciente_repository pacienteRepository,
            conteudo_clinico_repository conteudoRepository,
            pergunta_repository perguntaRepository,
            alternativa_pergunta_repository alternativaRepository,
            CasoClinicoMapper mapper,
            PerguntaMapper perguntaMapper,
            TentativaCasoService tentativaCasoService) {
        this.repository = repository;
        this.professorRepository = professorRepository;
        this.pacienteRepository = pacienteRepository;
        this.conteudoRepository = conteudoRepository;
        this.perguntaRepository = perguntaRepository;
        this.alternativaRepository = alternativaRepository;
        this.mapper = mapper;
        this.perguntaMapper = perguntaMapper;
        this.tentativaCasoService = tentativaCasoService;
    }

    public Page<caso_clinico_response_DTO> listarPaginado(
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

    public Page<caso_clinico_response_DTO> listarPublicados(Pageable pageable) {
        return repository.findByStatus(StatusCasoClinico.PUBLICADO, pageable).map(mapper::toResponse);
    }

    public Page<caso_clinico_response_DTO> listarPorProfessor(Long idProfessor, Pageable pageable) {
        if (!professorRepository.existsById(idProfessor)) {
            throw new RecursoNaoEncontradoException("Professor nao encontrado");
        }

        return repository.findByProfessorId(idProfessor, pageable).map(mapper::toResponse);
    }

    public caso_clinico_response_DTO buscarPorId(Long id) {
        return mapper.toResponse(buscarEntityPorId(id));
    }

    public caso_clinico_completo_DTO buscarCompletoPorId(Long id) {
        casos_clinicos caso = buscarEntityPorId(id);
        return montarCompleto(caso);
    }

    public caso_clinico_aluno_DTO buscarCompletoPublicadoPorId(Long id, Long idAluno) {
        casos_clinicos caso = buscarEntityPorId(id);
        if (caso.getStatus() != StatusCasoClinico.PUBLICADO) {
            throw new BusinessException("O caso clinico ainda nao esta publicado");
        }

        TentativaCaso tentativa = tentativaCasoService.iniciarOuBuscar(idAluno, caso);
        return montarCompletoParaAluno(caso, tentativa);
    }

    public caso_clinico_response_DTO salvar(caso_clinico_request_DTO dto) {
        return salvar(dto, dto.getIdProfessor());
    }

    public caso_clinico_response_DTO salvar(caso_clinico_request_DTO dto, Long idProfessorAutorizado) {
        Professor professor = buscarProfessorObrigatorio(idProfessorAutorizado);
        casos_clinicos caso = mapper.toEntity(dto, professor);
        return mapper.toResponse(repository.save(caso));
    }

    public caso_clinico_response_DTO atualizar(Long id, caso_clinico_request_DTO dto) {
        return atualizar(id, dto, dto.getIdProfessor());
    }

    public caso_clinico_response_DTO atualizar(Long id, caso_clinico_request_DTO dto, Long idProfessorAutorizado) {
        casos_clinicos caso = buscarEntityPorId(id);
        Professor professor = idProfessorAutorizado != null ? buscarProfessor(idProfessorAutorizado) : null;
        mapper.updateEntity(dto, caso, professor);
        return mapper.toResponse(repository.save(caso));
    }

    public caso_clinico_response_DTO publicar(Long id) {
        casos_clinicos caso = buscarEntityPorId(id);
        caso.setStatus(StatusCasoClinico.PUBLICADO);
        return mapper.toResponse(repository.save(caso));
    }

    public void deletar(Long id) {
        buscarEntityPorId(id);
        repository.deleteById(id);
    }

    private caso_clinico_completo_DTO montarCompleto(casos_clinicos caso) {
        Long id = caso.getIdCaso();

        List<paciente_DTO> pacientes = pacienteRepository.findByCasoClinicoIdCaso(id)
                .stream()
                .map(this::paraPacienteDTO)
                .toList();

        List<conteudo_clinico_DTO> conteudos = conteudoRepository.findByCasoClinicoIdCaso(id)
                .stream()
                .map(this::paraConteudoDTO)
                .toList();

        List<pergunta_response_DTO> perguntas = perguntaRepository.findByCasoClinicoIdCaso(id)
                .stream()
                .map(this::paraPerguntaDTO)
                .toList();

        return new caso_clinico_completo_DTO(mapper.toResponse(caso), pacientes, conteudos, perguntas);
    }

    private caso_clinico_aluno_DTO montarCompletoParaAluno(
            casos_clinicos caso,
            TentativaCaso tentativa) {
        Long id = caso.getIdCaso();

        List<paciente_DTO> pacientes = pacienteRepository.findByCasoClinicoIdCaso(id)
                .stream()
                .map(this::paraPacienteDTO)
                .toList();

        List<conteudo_clinico_aluno_DTO> conteudos = conteudoRepository.findByCasoClinicoIdCaso(id)
                .stream()
                .map(this::paraConteudoAlunoDTO)
                .toList();

        List<pergunta_aluno_DTO> perguntas = perguntaRepository.findByCasoClinicoIdCaso(id)
                .stream()
                .map(this::paraPerguntaAlunoDTO)
                .toList();

        return new caso_clinico_aluno_DTO(
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

    private paciente_DTO paraPacienteDTO(paciente paciente) {
        paciente_DTO dto = new paciente_DTO();
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

    private conteudo_clinico_DTO paraConteudoDTO(conteudo_clinico conteudo) {
        conteudo_clinico_DTO dto = new conteudo_clinico_DTO();
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

    private conteudo_clinico_aluno_DTO paraConteudoAlunoDTO(conteudo_clinico conteudo) {
        Long idCaso = conteudo.getCasoClinico() != null ? conteudo.getCasoClinico().getIdCaso() : null;
        return new conteudo_clinico_aluno_DTO(
                conteudo.getIdConteudo(),
                idCaso,
                conteudo.getSintomas(),
                conteudo.getContexto(),
                conteudo.getExamClinico(),
                conteudo.getAntecClinico());
    }

    private pergunta_response_DTO paraPerguntaDTO(pergunta pergunta) {
        return perguntaMapper.toResponse(pergunta, buscarAlternativasDTO(pergunta));
    }

    private pergunta_aluno_DTO paraPerguntaAlunoDTO(pergunta pergunta) {
        Long idCaso = pergunta.getCasoClinico() != null ? pergunta.getCasoClinico().getIdCaso() : null;
        List<alternativa_aluno_DTO> alternativas = buscarAlternativasDTO(pergunta)
                .stream()
                .map(alternativa -> new alternativa_aluno_DTO(
                        alternativa.getId(),
                        alternativa.getLetra(),
                        alternativa.getTexto()))
                .toList();

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

    private List<alternativa_pergunta_DTO> buscarAlternativasDTO(pergunta pergunta) {
        List<alternativa_pergunta_DTO> alternativas = alternativaRepository.findByPerguntaIdOrderByLetra(pergunta.getId())
                .stream()
                .map(this::paraAlternativaDTO)
                .toList();

        if (!alternativas.isEmpty()) {
            return alternativas;
        }

        return montarAlternativasLegadas(pergunta);
    }

    private alternativa_pergunta_DTO paraAlternativaDTO(AlternativaPergunta alternativa) {
        return new alternativa_pergunta_DTO(
                alternativa.getId(),
                alternativa.getLetra(),
                alternativa.getTexto(),
                alternativa.getCorreta());
    }

    private List<alternativa_pergunta_DTO> montarAlternativasLegadas(pergunta pergunta) {
        List<alternativa_pergunta_DTO> alternativas = new ArrayList<>();

        adicionarAlternativaLegada(alternativas, "A", pergunta.getAlternativaA(), pergunta);
        adicionarAlternativaLegada(alternativas, "B", pergunta.getAlternativaB(), pergunta);
        adicionarAlternativaLegada(alternativas, "C", pergunta.getAlternativaC(), pergunta);
        adicionarAlternativaLegada(alternativas, "D", pergunta.getAlternativaD(), pergunta);
        adicionarAlternativaLegada(alternativas, "E", pergunta.getAlternativaE(), pergunta);

        return alternativas;
    }

    private void adicionarAlternativaLegada(
            List<alternativa_pergunta_DTO> alternativas,
            String letra,
            String texto,
            pergunta pergunta) {
        if (texto == null || texto.isBlank()) {
            return;
        }

        boolean correta = corresponde(letra, pergunta.getGabarito()) || corresponde(letra, pergunta.getResposta());
        alternativas.add(new alternativa_pergunta_DTO(null, letra, texto, correta));
    }

    private boolean corresponde(String valor, String referencia) {
        return valor != null && referencia != null && valor.trim().equalsIgnoreCase(referencia.trim());
    }

    private Specification<casos_clinicos> filtrarCasos(
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

    private casos_clinicos buscarEntityPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Caso clinico nao encontrado"));
    }
}
