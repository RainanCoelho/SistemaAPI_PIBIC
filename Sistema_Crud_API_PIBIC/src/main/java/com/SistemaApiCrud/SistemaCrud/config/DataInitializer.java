package com.SistemaApiCrud.SistemaCrud.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.SistemaApiCrud.SistemaCrud.entity.Aluno;
import com.SistemaApiCrud.SistemaCrud.entity.Professor;
import com.SistemaApiCrud.SistemaCrud.entity.Usuario;
import com.SistemaApiCrud.SistemaCrud.entity.enums.PapelUsuario;
import com.SistemaApiCrud.SistemaCrud.repository.aluno_repository;
import com.SistemaApiCrud.SistemaCrud.repository.professor_repository;
import com.SistemaApiCrud.SistemaCrud.repository.usuario_repository;

@Component
@Profile({"dev", "test"})
public class DataInitializer implements CommandLineRunner {

    private final usuario_repository usuarioRepository;
    private final aluno_repository alunoRepository;
    private final professor_repository professorRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.admin.username}")
    private String adminUsername;

    @Value("${app.security.admin.password}")
    private String adminPassword;

    @Value("${app.security.professor.username}")
    private String professorUsername;

    @Value("${app.security.professor.password}")
    private String professorPassword;

    @Value("${app.security.aluno.username}")
    private String alunoUsername;

    @Value("${app.security.aluno.password}")
    private String alunoPassword;

    public DataInitializer(
            usuario_repository usuarioRepository,
            aluno_repository alunoRepository,
            professor_repository professorRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.alunoRepository = alunoRepository;
        this.professorRepository = professorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        criarUsuarioInicial(adminUsername, adminPassword, PapelUsuario.ADMIN);
        Usuario professor = criarUsuarioInicial(professorUsername, professorPassword, PapelUsuario.PROFESSOR);
        Usuario aluno = criarUsuarioInicial(alunoUsername, alunoPassword, PapelUsuario.ALUNO);

        vincularProfessorInicial(professor);
        vincularAlunoInicial(aluno);
    }

    private Usuario criarUsuarioInicial(String username, String senha, PapelUsuario role) {
        return usuarioRepository.findByUsername(username)
                .orElseGet(() -> {
                    Usuario usuario = new Usuario();
                    usuario.setUsername(username);
                    usuario.setSenha(passwordEncoder.encode(senha));
                    usuario.setRole(role);
                    usuario.setAtivo(true);
                    return usuarioRepository.save(usuario);
                });
    }

    private void vincularProfessorInicial(Usuario usuario) {
        if (usuario.getProfessor() != null) {
            return;
        }

        Professor professor = new Professor();
        professor.setNome("Professor Inicial");
        professor.setEmail("professor.inicial@example.com");
        professor.setMateria("Clinica Medica");

        usuario.setProfessor(professorRepository.save(professor));
        usuarioRepository.save(usuario);
    }

    private void vincularAlunoInicial(Usuario usuario) {
        if (usuario.getAluno() != null) {
            return;
        }

        Aluno aluno = new Aluno();
        aluno.setNome("Aluno Inicial");
        aluno.setEmail("aluno.inicial@example.com");
        aluno.setCurso("Medicina");
        aluno.setPeriodo("1");

        usuario.setAluno(alunoRepository.save(aluno));
        usuarioRepository.save(usuario);
    }
}
