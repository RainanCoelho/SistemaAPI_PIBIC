package com.SistemaApiCrud.SistemaCrud.config;

import java.io.IOException;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.SistemaApiCrud.SistemaCrud.service.JwtService;
import com.SistemaApiCrud.SistemaCrud.repository.usuario_repository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final usuario_repository usuarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService, usuario_repository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");

        if (authorization != null
                && authorization.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = authorization.substring(7);

            try {
                if (jwtService.isTokenValido(token)) {
                    usuarioRepository.findByUsername(jwtService.getUsername(token))
                            .filter(usuario -> Boolean.TRUE.equals(usuario.getAtivo()))
                            .filter(usuario -> {
                                long versaoAtual = usuario.getVersaoCredencial() == null
                                        ? 0L
                                        : usuario.getVersaoCredencial();
                                return versaoAtual == jwtService.getVersaoCredencial(token);
                            })
                            .ifPresent(usuario -> {
                                var authentication = jwtService.criarAuthentication(token, usuario);
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                            });
                }
            } catch (RuntimeException ignored) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
