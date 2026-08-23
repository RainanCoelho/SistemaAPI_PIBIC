package com.SistemaApiCrud.SistemaCrud.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import com.SistemaApiCrud.SistemaCrud.dto.PacienteDTO;
import com.SistemaApiCrud.SistemaCrud.entity.CasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.Paciente;
import com.SistemaApiCrud.SistemaCrud.exception.BadRequestException;
import com.SistemaApiCrud.SistemaCrud.exception.RecursoNaoEncontradoException;
import com.SistemaApiCrud.SistemaCrud.repository.PacienteRepository;

@Service
public class PacienteService {

    private final PacienteRepository repository;
    private final CasoClinicoLockService casoLockService;

    public PacienteService(
            PacienteRepository repository,
            CasoClinicoLockService casoLockService) {
        this.repository = repository;
        this.casoLockService = casoLockService;
    }

    public Page<PacienteDTO> listar(Pageable pageable, Long idProfessor) {
        if (idProfessor == null) {
            return repository.findAll(pageable).map(this::paraDTO);
        }
        return repository.findByCasoClinicoProfessorId(idProfessor, pageable).map(this::paraDTO);
    }

    public PacienteDTO buscarPorId(Long id) {
        return paraDTO(buscarEntityPorId(id));
    }

    @Transactional
    public PacienteDTO salvar(PacienteDTO dto) {
        Paciente paciente = new Paciente();
        CasoClinico caso = casoLockService.bloquearRascunho(idCasoObrigatorio(dto));
        aplicarDados(dto, paciente, caso);
        Paciente pacienteSalvo = repository.save(paciente);
        return paraDTO(pacienteSalvo);
    }

    @Transactional
    public PacienteDTO atualizar(Long id, PacienteDTO dto) {
        Paciente paciente = buscarEntityPorIdParaAtualizacao(id);
        Long idCasoAtual = paciente.getCasoClinico().getIdCaso();
        Long idCasoDestino = idCasoObrigatorio(dto);
        Map<Long, CasoClinico> casos = casoLockService.bloquearRascunhos(
                List.of(idCasoAtual, idCasoDestino));
        aplicarDados(dto, paciente, casos.get(idCasoDestino));
        return paraDTO(paciente);
    }

    @Transactional
    public void deletar(Long id) {
        Paciente paciente = buscarEntityPorIdParaAtualizacao(id);
        casoLockService.bloquearRascunho(paciente.getCasoClinico().getIdCaso());
        repository.deleteById(id);
    }

    private PacienteDTO paraDTO(Paciente paciente) {
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

    private void aplicarDados(
            PacienteDTO dto,
            Paciente paciente,
            CasoClinico caso) {
        paciente.setCasoClinico(caso);
        paciente.setNome(dto.getNome());
        paciente.setProfissao(dto.getProfissao());
        paciente.setSexo(dto.getSexo());
        paciente.setIdade(dto.getIdade());
        paciente.setEstadoCivil(dto.getEstadoCivil());
        paciente.setAltura(dto.getAltura());
        paciente.setPeso(dto.getPeso());
    }

    private Long idCasoObrigatorio(PacienteDTO dto) {
        if (dto.getIdCaso() == null) {
            throw new BadRequestException("O caso clinico e obrigatorio");
        }
        return dto.getIdCaso();
    }

    private Paciente buscarEntityPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paciente nao encontrado"));
    }

    private Paciente buscarEntityPorIdParaAtualizacao(Long id) {
        return repository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paciente nao encontrado"));
    }
}
