package com.SistemaApiCrud.SistemaCrud.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.SistemaApiCrud.SistemaCrud.entity.Usuario;
import com.SistemaApiCrud.SistemaCrud.repository.usuario_repository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final usuario_repository repository;

    public CustomUserDetailsService(usuario_repository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = repository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado"));

        return User.withUsername(usuario.getUsername())
                .password(usuario.getSenha())
                .roles(usuario.getRole().name())
                .disabled(!Boolean.TRUE.equals(usuario.getAtivo()))
                .build();
    }
}
