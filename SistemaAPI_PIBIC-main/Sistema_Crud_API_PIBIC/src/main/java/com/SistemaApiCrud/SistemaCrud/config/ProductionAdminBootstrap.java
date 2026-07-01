package com.SistemaApiCrud.SistemaCrud.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.SistemaApiCrud.SistemaCrud.entity.Usuario;
import com.SistemaApiCrud.SistemaCrud.entity.enums.PapelUsuario;
import com.SistemaApiCrud.SistemaCrud.repository.usuario_repository;

@Component
@Profile("prod")
@ConditionalOnProperty(
        name = "app.security.bootstrap-admin.enabled",
        havingValue = "true")
public class ProductionAdminBootstrap implements CommandLineRunner {

    private final usuario_repository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public ProductionAdminBootstrap(
            usuario_repository usuarioRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.security.bootstrap-admin.username}") String username,
            @Value("${app.security.bootstrap-admin.password}") String password) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username == null ? null : username.trim();
        this.password = password;
    }

    @Override
    public void run(String... args) {
        validarCredenciais();

        if (usuarioRepository.existsByUsername(username)) {
            return;
        }

        Usuario admin = new Usuario();
        admin.setUsername(username.trim());
        admin.setSenha(passwordEncoder.encode(password));
        admin.setRole(PapelUsuario.ADMIN);
        admin.setAtivo(true);
        admin.setVersaoCredencial(0L);
        usuarioRepository.save(admin);
    }

    private void validarCredenciais() {
        if (username == null || username.isBlank() || username.length() > 100) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_USER deve ter entre 1 e 100 caracteres");
        }
        if (password == null || password.length() < 12 || password.length() > 72) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD deve ter entre 12 e 72 caracteres");
        }
    }
}
