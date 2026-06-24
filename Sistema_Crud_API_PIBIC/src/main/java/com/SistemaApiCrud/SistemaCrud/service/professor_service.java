package com.SistemaApiCrud.SistemaCrud.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.SistemaApiCrud.SistemaCrud.DTO.professor_cadastro_request_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.professor_request_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.professor_response_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.usuario_request_DTO;
import com.SistemaApiCrud.SistemaCrud.entity.Professor;
import com.SistemaApiCrud.SistemaCrud.entity.enums.PapelUsuario;
import com.SistemaApiCrud.SistemaCrud.exception.RecursoNaoEncontradoException;
import com.SistemaApiCrud.SistemaCrud.mapper.ProfessorMapper;
import com.SistemaApiCrud.SistemaCrud.repository.professor_repository;

@Service
public class professor_service {

    private final professor_repository repository;
    private final ProfessorMapper mapper;
    private final usuario_service usuarioService;

    public professor_service(professor_repository repository, ProfessorMapper mapper, usuario_service usuarioService) {
        this.repository = repository;
        this.mapper = mapper;
        this.usuarioService = usuarioService;
    }

    public Page<professor_response_DTO> listar(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    public professor_response_DTO buscarPorId(Long id) {
        return mapper.toResponse(buscarEntityPorId(id));
    }

    public professor_response_DTO salvar(professor_request_DTO dto) {
        Professor professor = mapper.toEntity(dto);
        return mapper.toResponse(repository.save(professor));
    }

    @Transactional
    public professor_response_DTO cadastrarPublico(professor_cadastro_request_DTO dto) {
        usuarioService.validarUsernameDisponivel(dto.getUsername());

        professor_request_DTO professorRequest = new professor_request_DTO(
                dto.getNome(),
                dto.getEmail(),
                dto.getMateria());

        professor_response_DTO professor = salvar(professorRequest);

        usuario_request_DTO usuarioRequest = new usuario_request_DTO(
                dto.getUsername(),
                dto.getSenha(),
                PapelUsuario.PROFESSOR,
                true,
                null,
                professor.getId());

        usuarioService.salvar(usuarioRequest);
        return professor;
    }

    public professor_response_DTO atualizar(Long id, professor_request_DTO dto) {
        Professor professor = buscarEntityPorId(id);
        mapper.updateEntity(dto, professor);
        return mapper.toResponse(repository.save(professor));
    }

    public void deletar(Long id) {
        buscarEntityPorId(id);
        repository.deleteById(id);
    }

    private Professor buscarEntityPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Professor nao encontrado"));
    }
}
