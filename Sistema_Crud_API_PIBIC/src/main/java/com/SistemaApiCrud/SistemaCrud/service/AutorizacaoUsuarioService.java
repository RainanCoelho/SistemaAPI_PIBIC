package com.SistemaApiCrud.SistemaCrud.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.SistemaApiCrud.SistemaCrud.entity.Usuario;
import com.SistemaApiCrud.SistemaCrud.entity.CasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.enums.PapelUsuario;
import com.SistemaApiCrud.SistemaCrud.entity.Pergunta;
import com.SistemaApiCrud.SistemaCrud.exception.RecursoNaoEncontradoException;
import com.SistemaApiCrud.SistemaCrud.repository.CasoClinicoRepository;
import com.SistemaApiCrud.SistemaCrud.repository.ConteudoClinicoRepository;
import com.SistemaApiCrud.SistemaCrud.repository.PacienteRepository;
import com.SistemaApiCrud.SistemaCrud.repository.PerguntaRepository;
import com.SistemaApiCrud.SistemaCrud.repository.UsuarioRepository;

@Service
public class AutorizacaoUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final CasoClinicoRepository casoRepository;
    private final PerguntaRepository perguntaRepository;
    private final PacienteRepository pacienteRepository;
    private final ConteudoClinicoRepository conteudoRepository;

    public AutorizacaoUsuarioService(
            UsuarioRepository usuarioRepository,
            CasoClinicoRepository casoRepository,
            PerguntaRepository perguntaRepository,
            PacienteRepository pacienteRepository,
            ConteudoClinicoRepository conteudoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.casoRepository = casoRepository;
        this.perguntaRepository = perguntaRepository;
        this.pacienteRepository = pacienteRepository;
        this.conteudoRepository = conteudoRepository;
    }

    public boolean isAdmin() {
        return getUsuarioAutenticado().getRole() == PapelUsuario.ADMIN;
    }

    public Long getIdUsuarioAutenticado() {
        return getUsuarioAutenticado().getId();
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
        CasoClinico caso = casoRepository.findById(idCaso)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Caso clinico nao encontrado"));
        validarAcessoCaso(caso);
    }

    public void validarAcessoCaso(CasoClinico caso) {
        Usuario usuario = getUsuarioAutenticado();
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
        Pergunta pergunta = perguntaRepository.findById(idPergunta)
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
