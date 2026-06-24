package com.SistemaApiCrud.SistemaCrud.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.SistemaApiCrud.SistemaCrud.entity.Usuario;
import com.SistemaApiCrud.SistemaCrud.entity.casos_clinicos;
import com.SistemaApiCrud.SistemaCrud.entity.enums.PapelUsuario;
import com.SistemaApiCrud.SistemaCrud.entity.pergunta;
import com.SistemaApiCrud.SistemaCrud.exception.RecursoNaoEncontradoException;
import com.SistemaApiCrud.SistemaCrud.repository.caso_clinico_repository;
import com.SistemaApiCrud.SistemaCrud.repository.conteudo_clinico_repository;
import com.SistemaApiCrud.SistemaCrud.repository.paciente_repository;
import com.SistemaApiCrud.SistemaCrud.repository.pergunta_repository;
import com.SistemaApiCrud.SistemaCrud.repository.usuario_repository;

@Service
public class AutorizacaoUsuarioService {

    private final usuario_repository usuarioRepository;
    private final caso_clinico_repository casoRepository;
    private final pergunta_repository perguntaRepository;
    private final paciente_repository pacienteRepository;
    private final conteudo_clinico_repository conteudoRepository;

    public AutorizacaoUsuarioService(
            usuario_repository usuarioRepository,
            caso_clinico_repository casoRepository,
            pergunta_repository perguntaRepository,
            paciente_repository pacienteRepository,
            conteudo_clinico_repository conteudoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.casoRepository = casoRepository;
        this.perguntaRepository = perguntaRepository;
        this.pacienteRepository = pacienteRepository;
        this.conteudoRepository = conteudoRepository;
    }

    public boolean isAdmin() {
        return getUsuarioAutenticado().getRole() == PapelUsuario.ADMIN;
    }

    public Long getIdProfessorAutenticado() {
        Usuario usuario = getUsuarioAutenticado();
        if (usuario.getRole() != PapelUsuario.PROFESSOR || usuario.getProfessor() == null) {
            negar();
        }

        return usuario.getProfessor().getId();
    }

    public Long resolverFiltroProfessor(Long idProfessorSolicitado) {
        Usuario usuario = getUsuarioAutenticado();
        if (usuario.getRole() == PapelUsuario.ADMIN) {
            return idProfessorSolicitado;
        }

        if (usuario.getRole() == PapelUsuario.PROFESSOR && usuario.getProfessor() != null) {
            Long idProfessor = usuario.getProfessor().getId();
            if (idProfessorSolicitado != null && !idProfessor.equals(idProfessorSolicitado)) {
                negar();
            }
            return idProfessor;
        }

        negar();
        return null;
    }

    public Long resolverProfessorParaEscrita(Long idProfessorSolicitado) {
        Usuario usuario = getUsuarioAutenticado();
        if (usuario.getRole() == PapelUsuario.ADMIN) {
            return idProfessorSolicitado;
        }

        if (usuario.getRole() == PapelUsuario.PROFESSOR && usuario.getProfessor() != null) {
            Long idProfessor = usuario.getProfessor().getId();
            if (idProfessorSolicitado != null && !idProfessor.equals(idProfessorSolicitado)) {
                negar();
            }
            return idProfessor;
        }

        negar();
        return null;
    }

    public void validarAcessoAluno(Long idAluno) {
        Usuario usuario = getUsuarioAutenticado();
        if (usuario.getRole() == PapelUsuario.ADMIN) {
            return;
        }

        if (usuario.getRole() == PapelUsuario.ALUNO
                && usuario.getAluno() != null
                && usuario.getAluno().getIdAluno().equals(idAluno)) {
            return;
        }

        negar();
    }

    public void validarAcessoProfessor(Long idProfessor) {
        Usuario usuario = getUsuarioAutenticado();
        if (usuario.getRole() == PapelUsuario.ADMIN) {
            return;
        }

        if (usuario.getRole() == PapelUsuario.PROFESSOR
                && usuario.getProfessor() != null
                && usuario.getProfessor().getId().equals(idProfessor)) {
            return;
        }

        negar();
    }

    public void validarAcessoCaso(Long idCaso) {
        Usuario usuario = getUsuarioAutenticado();
        casos_clinicos caso = casoRepository.findById(idCaso)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Caso clinico nao encontrado"));

        if (usuario.getRole() == PapelUsuario.ADMIN) {
            return;
        }

        if (usuario.getRole() == PapelUsuario.PROFESSOR
                && usuario.getProfessor() != null
                && caso.getProfessor() != null
                && usuario.getProfessor().getId().equals(caso.getProfessor().getId())) {
            return;
        }

        negar();
    }

    public void validarAcessoPergunta(Long idPergunta) {
        pergunta pergunta = perguntaRepository.findById(idPergunta)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pergunta nao encontrada"));

        if (pergunta.getCasoClinico() == null) {
            negar();
        }

        validarAcessoCaso(pergunta.getCasoClinico().getIdCaso());
    }

    public void validarAcessoPaciente(Long idPaciente) {
        var paciente = pacienteRepository.findById(idPaciente)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paciente nao encontrado"));
        if (paciente.getCasoClinico() == null) {
            negar();
        }
        validarAcessoCaso(paciente.getCasoClinico().getIdCaso());
    }

    public void validarAcessoConteudo(Long idConteudo) {
        var conteudo = conteudoRepository.findById(idConteudo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conteudo clinico nao encontrado"));
        if (conteudo.getCasoClinico() == null) {
            negar();
        }
        validarAcessoCaso(conteudo.getCasoClinico().getIdCaso());
    }

    private Usuario getUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            negar();
        }

        return usuarioRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Acesso negado"));
    }

    private void negar() {
        throw new AccessDeniedException("Acesso negado");
    }
}
