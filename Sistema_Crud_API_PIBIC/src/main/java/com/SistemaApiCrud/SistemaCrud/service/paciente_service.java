package com.SistemaApiCrud.SistemaCrud.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import com.SistemaApiCrud.SistemaCrud.DTO.paciente_DTO;
import com.SistemaApiCrud.SistemaCrud.entity.casos_clinicos;
import com.SistemaApiCrud.SistemaCrud.entity.paciente;
import com.SistemaApiCrud.SistemaCrud.exception.BadRequestException;
import com.SistemaApiCrud.SistemaCrud.exception.RecursoNaoEncontradoException;
import com.SistemaApiCrud.SistemaCrud.repository.paciente_repository;

@Service
public class paciente_service {

    private final paciente_repository repository;
    private final CasoClinicoLockService casoLockService;

    public paciente_service(
            paciente_repository repository,
            CasoClinicoLockService casoLockService) {
        this.repository = repository;
        this.casoLockService = casoLockService;
    }

    public Page<paciente_DTO> listar(Pageable pageable, Long idProfessor) {
        if (idProfessor == null) {
            return repository.findAll(pageable).map(this::paraDTO);
        }
        return repository.findByCasoClinicoProfessorId(idProfessor, pageable).map(this::paraDTO);
    }

    public paciente_DTO buscarPorId(Long id) {
        return paraDTO(buscarEntityPorId(id));
    }

    @Transactional
    public paciente_DTO salvar(paciente_DTO dto) {
        paciente paciente = new paciente();
        casos_clinicos caso = casoLockService.bloquearRascunho(idCasoObrigatorio(dto));
        aplicarDados(dto, paciente, caso);
        paciente pacienteSalvo = repository.save(paciente);
        return paraDTO(pacienteSalvo);
    }

    @Transactional
    public paciente_DTO atualizar(Long id, paciente_DTO dto) {
        paciente paciente = buscarEntityPorIdParaAtualizacao(id);
        Long idCasoAtual = paciente.getCasoClinico().getIdCaso();
        Long idCasoDestino = idCasoObrigatorio(dto);
        Map<Long, casos_clinicos> casos = casoLockService.bloquearRascunhos(
                List.of(idCasoAtual, idCasoDestino));
        aplicarDados(dto, paciente, casos.get(idCasoDestino));
        return paraDTO(paciente);
    }

    @Transactional
    public void deletar(Long id) {
        paciente paciente = buscarEntityPorIdParaAtualizacao(id);
        casoLockService.bloquearRascunho(paciente.getCasoClinico().getIdCaso());
        repository.deleteById(id);
    }

    private paciente_DTO paraDTO(paciente paciente) {
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

    private void aplicarDados(
            paciente_DTO dto,
            paciente paciente,
            casos_clinicos caso) {
        paciente.setCasoClinico(caso);
        paciente.setNome(dto.getNome());
        paciente.setProfissao(dto.getProfissao());
        paciente.setSexo(dto.getSexo());
        paciente.setIdade(dto.getIdade());
        paciente.setEstadoCivil(dto.getEstadoCivil());
        paciente.setAltura(dto.getAltura());
        paciente.setPeso(dto.getPeso());
    }

    private Long idCasoObrigatorio(paciente_DTO dto) {
        if (dto.getIdCaso() == null) {
            throw new BadRequestException("O caso clinico e obrigatorio");
        }
        return dto.getIdCaso();
    }

    private paciente buscarEntityPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paciente nao encontrado"));
    }

    private paciente buscarEntityPorIdParaAtualizacao(Long id) {
        return repository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paciente nao encontrado"));
    }
}
