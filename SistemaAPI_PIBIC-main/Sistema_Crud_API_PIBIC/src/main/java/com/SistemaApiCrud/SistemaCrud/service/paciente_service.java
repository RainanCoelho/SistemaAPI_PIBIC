package com.SistemaApiCrud.SistemaCrud.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.SistemaApiCrud.SistemaCrud.DTO.paciente_DTO;
import com.SistemaApiCrud.SistemaCrud.entity.casos_clinicos;
import com.SistemaApiCrud.SistemaCrud.entity.paciente;
import com.SistemaApiCrud.SistemaCrud.exception.RecursoNaoEncontradoException;
import com.SistemaApiCrud.SistemaCrud.repository.caso_clinico_repository;
import com.SistemaApiCrud.SistemaCrud.repository.paciente_repository;

@Service
public class paciente_service {

    @Autowired
    private paciente_repository repository;

    @Autowired
    private caso_clinico_repository casoRepository;

    public Page<paciente_DTO> listar(Pageable pageable, Long idProfessor) {
        if (idProfessor == null) {
            return repository.findAll(pageable).map(this::paraDTO);
        }
        return repository.findByCasoClinicoProfessorId(idProfessor, pageable).map(this::paraDTO);
    }

    public paciente_DTO buscarPorId(Long id) {
        return paraDTO(buscarEntityPorId(id));
    }

    public paciente_DTO salvar(paciente_DTO dto) {
        paciente paciente = paraEntity(dto);
        paciente pacienteSalvo = repository.save(paciente);
        return paraDTO(pacienteSalvo);
    }

    public paciente_DTO atualizar(Long id, paciente_DTO dto) {
        buscarEntityPorId(id);

        paciente paciente = paraEntity(dto);
        paciente.setIdPaciente(id);

        paciente pacienteAtualizado = repository.save(paciente);
        return paraDTO(pacienteAtualizado);
    }

    public void deletar(Long id) {
        buscarEntityPorId(id);
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

    private paciente paraEntity(paciente_DTO dto) {
        paciente paciente = new paciente();

        paciente.setIdPaciente(dto.getIdPaciente());

        if (dto.getIdCaso() != null) {
            casos_clinicos caso = casoRepository.findById(dto.getIdCaso())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Caso clinico nao encontrado"));
            paciente.setCasoClinico(caso);
        }

        paciente.setNome(dto.getNome());
        paciente.setProfissao(dto.getProfissao());
        paciente.setSexo(dto.getSexo());
        paciente.setIdade(dto.getIdade());
        paciente.setEstadoCivil(dto.getEstadoCivil());
        paciente.setAltura(dto.getAltura());
        paciente.setPeso(dto.getPeso());

        return paciente;
    }

    private paciente buscarEntityPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paciente nao encontrado"));
    }
}
