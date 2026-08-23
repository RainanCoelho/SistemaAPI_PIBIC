package com.SistemaApiCrud.SistemaCrud.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.SistemaApiCrud.SistemaCrud.dto.AlunoRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.AlunoResponseDTO;
import com.SistemaApiCrud.SistemaCrud.entity.Aluno;
import com.SistemaApiCrud.SistemaCrud.exception.RecursoNaoEncontradoException;
import com.SistemaApiCrud.SistemaCrud.mapper.AlunoMapper;
import com.SistemaApiCrud.SistemaCrud.repository.AlunoRepository;

@Service
public class AlunoService {

    private final AlunoRepository repository;
    private final AlunoMapper mapper;

    public AlunoService(AlunoRepository repository, AlunoMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public Page<AlunoResponseDTO> listar(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    public AlunoResponseDTO buscarPorId(Long id) {
        return mapper.toResponse(buscarEntityPorId(id));
    }

    public AlunoResponseDTO salvar(AlunoRequestDTO dto) {
        Aluno aluno = mapper.toEntity(dto);
        return mapper.toResponse(repository.save(aluno));
    }

    public AlunoResponseDTO atualizar(Long id, AlunoRequestDTO dto) {
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
