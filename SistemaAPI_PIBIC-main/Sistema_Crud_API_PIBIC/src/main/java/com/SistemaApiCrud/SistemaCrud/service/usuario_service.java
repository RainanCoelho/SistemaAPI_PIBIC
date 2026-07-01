package com.SistemaApiCrud.SistemaCrud.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.SistemaApiCrud.SistemaCrud.DTO.usuario_request_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.usuario_response_DTO;
import com.SistemaApiCrud.SistemaCrud.entity.Aluno;
import com.SistemaApiCrud.SistemaCrud.entity.Professor;
import com.SistemaApiCrud.SistemaCrud.entity.Usuario;
import com.SistemaApiCrud.SistemaCrud.entity.enums.PapelUsuario;
import com.SistemaApiCrud.SistemaCrud.exception.BusinessException;
import com.SistemaApiCrud.SistemaCrud.exception.RecursoNaoEncontradoException;
import com.SistemaApiCrud.SistemaCrud.mapper.UsuarioMapper;
import com.SistemaApiCrud.SistemaCrud.repository.aluno_repository;
import com.SistemaApiCrud.SistemaCrud.repository.professor_repository;
import com.SistemaApiCrud.SistemaCrud.repository.usuario_repository;

@Service
public class usuario_service {

    private final usuario_repository repository;
    private final aluno_repository alunoRepository;
    private final professor_repository professorRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper mapper;

    public usuario_service(
            usuario_repository repository,
            aluno_repository alunoRepository,
            professor_repository professorRepository,
            PasswordEncoder passwordEncoder,
            UsuarioMapper mapper) {
        this.repository = repository;
        this.alunoRepository = alunoRepository;
        this.professorRepository = professorRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
    }

    public Page<usuario_response_DTO> listar(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    public usuario_response_DTO buscarPorId(Long id) {
        return mapper.toResponse(buscarEntityPorId(id));
    }

    public Usuario buscarPorUsername(String username) {
        return repository.findByUsername(username)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));
    }

    public void validarUsernameDisponivel(String username) {
        if (repository.existsByUsername(username)) {
            throw new BusinessException("Ja existe um usuario cadastrado com esse nome de acesso");
        }
    }

    public usuario_response_DTO salvar(usuario_request_DTO dto) {
        if (repository.existsByUsername(dto.getUsername())) {
            throw new BusinessException("Ja existe um usuario cadastrado com esse nome de acesso");
        }

        Usuario usuario = aplicarDados(dto, new Usuario());
        return mapper.toResponse(repository.save(usuario));
    }

    @Transactional
    public usuario_response_DTO atualizar(Long id, usuario_request_DTO dto) {
        Usuario usuario = buscarEntityPorId(id);

        repository.findByUsername(dto.getUsername())
                .filter(usuarioEncontrado -> !usuarioEncontrado.getId().equals(id))
                .ifPresent(usuarioEncontrado -> {
                    throw new BusinessException("Ja existe um usuario cadastrado com esse nome de acesso");
                });

        usuario.setVersaoCredencial(proximaVersao(usuario));
        return mapper.toResponse(repository.save(aplicarDados(dto, usuario)));
    }

    @Transactional
    public usuario_response_DTO ativar(Long id) {
        Usuario usuario = buscarEntityPorId(id);
        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            usuario.setAtivo(true);
            usuario.setVersaoCredencial(proximaVersao(usuario));
        }
        return mapper.toResponse(repository.save(usuario));
    }

    @Transactional
    public usuario_response_DTO desativar(Long id) {
        Usuario usuario = buscarEntityPorId(id);
        if (Boolean.TRUE.equals(usuario.getAtivo())) {
            usuario.setAtivo(false);
            usuario.setVersaoCredencial(proximaVersao(usuario));
        }
        return mapper.toResponse(repository.save(usuario));
    }

    private Usuario aplicarDados(usuario_request_DTO dto, Usuario usuario) {
        usuario.setUsername(dto.getUsername());
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        usuario.setRole(dto.getRole());
        usuario.setAtivo(dto.getAtivo() == null || dto.getAtivo());
        vincularPerfil(dto, usuario);
        return usuario;
    }

    private long proximaVersao(Usuario usuario) {
        return usuario.getVersaoCredencial() == null ? 1L : usuario.getVersaoCredencial() + 1L;
    }

    private void vincularPerfil(usuario_request_DTO dto, Usuario usuario) {
        usuario.setAluno(null);
        usuario.setProfessor(null);

        if (dto.getRole() == PapelUsuario.ADMIN) {
            validarSemPerfil(dto);
            return;
        }

        if (dto.getRole() == PapelUsuario.ALUNO) {
            if (dto.getIdAluno() == null) {
                throw new BusinessException("Usuarios com role ALUNO precisam estar vinculados a um aluno");
            }

            if (dto.getIdProfessor() != null) {
                throw new BusinessException("Usuarios ALUNO nao podem ser vinculados a professor");
            }

            Aluno aluno = alunoRepository.findById(dto.getIdAluno())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno nao encontrado"));

            validarAlunoDisponivel(usuario, aluno.getIdAluno());
            usuario.setAluno(aluno);
            return;
        }

        if (dto.getRole() == PapelUsuario.PROFESSOR) {
            if (dto.getIdProfessor() == null) {
                throw new BusinessException("Usuarios com role PROFESSOR precisam estar vinculados a um professor");
            }

            if (dto.getIdAluno() != null) {
                throw new BusinessException("Usuarios PROFESSOR nao podem ser vinculados a aluno");
            }

            Professor professor = professorRepository.findById(dto.getIdProfessor())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Professor nao encontrado"));

            validarProfessorDisponivel(usuario, professor.getId());
            usuario.setProfessor(professor);
        }
    }

    private void validarSemPerfil(usuario_request_DTO dto) {
        if (dto.getIdAluno() != null || dto.getIdProfessor() != null) {
            throw new BusinessException("Usuarios ADMIN nao devem ser vinculados a aluno ou professor");
        }
    }

    private void validarAlunoDisponivel(Usuario usuarioAtual, Long idAluno) {
        repository.findByAlunoIdAluno(idAluno)
                .filter(usuario -> usuarioAtual.getId() == null || !usuario.getId().equals(usuarioAtual.getId()))
                .ifPresent(usuario -> {
                    throw new BusinessException("Esse aluno ja esta vinculado a outro usuario");
                });
    }

    private void validarProfessorDisponivel(Usuario usuarioAtual, Long idProfessor) {
        repository.findByProfessorId(idProfessor)
                .filter(usuario -> usuarioAtual.getId() == null || !usuario.getId().equals(usuarioAtual.getId()))
                .ifPresent(usuario -> {
                    throw new BusinessException("Esse professor ja esta vinculado a outro usuario");
                });
    }

    private Usuario buscarEntityPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));
    }
}
