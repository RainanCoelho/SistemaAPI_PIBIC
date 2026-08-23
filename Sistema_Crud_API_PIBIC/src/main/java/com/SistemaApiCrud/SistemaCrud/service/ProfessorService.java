package com.SistemaApiCrud.SistemaCrud.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.SistemaApiCrud.SistemaCrud.dto.ProfessorCadastroRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.ProfessorRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.ProfessorResponseDTO;
import com.SistemaApiCrud.SistemaCrud.dto.UsuarioRequestDTO;
import com.SistemaApiCrud.SistemaCrud.entity.Professor;
import com.SistemaApiCrud.SistemaCrud.entity.enums.PapelUsuario;
import com.SistemaApiCrud.SistemaCrud.exception.RecursoNaoEncontradoException;
import com.SistemaApiCrud.SistemaCrud.mapper.ProfessorMapper;
import com.SistemaApiCrud.SistemaCrud.repository.ProfessorRepository;

@Service
public class ProfessorService {

    private final ProfessorRepository repository;
    private final ProfessorMapper mapper;
    private final UsuarioService usuarioService;

    public ProfessorService(ProfessorRepository repository, ProfessorMapper mapper, UsuarioService usuarioService) {
        this.repository = repository;
        this.mapper = mapper;
        this.usuarioService = usuarioService;
    }

    public Page<ProfessorResponseDTO> listar(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    public ProfessorResponseDTO buscarPorId(Long id) {
        return mapper.toResponse(buscarEntityPorId(id));
    }

    public ProfessorResponseDTO salvar(ProfessorRequestDTO dto) {
        Professor professor = mapper.toEntity(dto);
        return mapper.toResponse(repository.save(professor));
    }

    @Transactional
    public ProfessorResponseDTO cadastrarPublico(ProfessorCadastroRequestDTO dto) {
        usuarioService.validarUsernameDisponivel(dto.getUsername());

        ProfessorRequestDTO professorRequest = new ProfessorRequestDTO(
                dto.getNome(),
                dto.getEmail(),
                dto.getMateria());

        ProfessorResponseDTO professor = salvar(professorRequest);

        UsuarioRequestDTO usuarioRequest = new UsuarioRequestDTO(
                dto.getUsername(),
                dto.getSenha(),
                PapelUsuario.PROFESSOR,
                true,
                null,
                professor.getId());

        usuarioService.salvar(usuarioRequest);
        return professor;
    }

    public ProfessorResponseDTO atualizar(Long id, ProfessorRequestDTO dto) {
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
