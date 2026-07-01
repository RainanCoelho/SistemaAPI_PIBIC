package com.SistemaApiCrud.SistemaCrud.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.SistemaApiCrud.SistemaCrud.DTO.aluno_request_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.aluno_response_DTO;
import com.SistemaApiCrud.SistemaCrud.entity.Aluno;
import com.SistemaApiCrud.SistemaCrud.exception.RecursoNaoEncontradoException;
import com.SistemaApiCrud.SistemaCrud.mapper.AlunoMapper;
import com.SistemaApiCrud.SistemaCrud.repository.aluno_repository;

@Service
public class aluno_service {

    private final aluno_repository repository;
    private final AlunoMapper mapper;

    public aluno_service(aluno_repository repository, AlunoMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public Page<aluno_response_DTO> listar(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    public aluno_response_DTO buscarPorId(Long id) {
        return mapper.toResponse(buscarEntityPorId(id));
    }

    public aluno_response_DTO salvar(aluno_request_DTO dto) {
        Aluno aluno = mapper.toEntity(dto);
        return mapper.toResponse(repository.save(aluno));
    }

    public aluno_response_DTO atualizar(Long id, aluno_request_DTO dto) {
        Aluno aluno = buscarEntityPorId(id);
        mapper.updateEntity(dto, aluno);
        return mapper.toResponse(repository.save(aluno));
    }

    public void deletar(Long id) {
        buscarEntityPorId(id);
        repository.deleteById(id);
    }

    private Aluno buscarEntityPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno nao encontrado"));
    }
}
