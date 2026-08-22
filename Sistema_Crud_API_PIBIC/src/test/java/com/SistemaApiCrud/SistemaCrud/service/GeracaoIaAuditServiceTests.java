package com.SistemaApiCrud.SistemaCrud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.SistemaApiCrud.SistemaCrud.entity.AuditoriaGeracaoIa;
import com.SistemaApiCrud.SistemaCrud.entity.Usuario;
import com.SistemaApiCrud.SistemaCrud.entity.casos_clinicos;
import com.SistemaApiCrud.SistemaCrud.entity.enums.OperacaoGeracaoIa;
import com.SistemaApiCrud.SistemaCrud.repository.auditoria_geracao_ia_repository;
import com.SistemaApiCrud.SistemaCrud.repository.usuario_repository;

class GeracaoIaAuditServiceTests {

    private final auditoria_geracao_ia_repository repository =
            mock(auditoria_geracao_ia_repository.class);
    private final usuario_repository usuarioRepository = mock(usuario_repository.class);

    @AfterEach
    void limparAutenticacao() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveRegistrarSomenteHashesEMetadadosDeProveniencia() {
        Usuario usuario = new Usuario();
        usuario.setId(9L);
        usuario.setUsername("professor-auditor");
        casos_clinicos caso = new casos_clinicos();
        caso.setIdCaso(7L);
        when(usuarioRepository.findByUsername("professor-auditor"))
                .thenReturn(Optional.of(usuario));
        when(repository.save(any(AuditoriaGeracaoIa.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "professor-auditor",
                        null,
                        java.util.List.of()));
        GeracaoIaAuditService service = new GeracaoIaAuditService(
                repository,
                usuarioRepository,
                "http://gateway-interno:3001/v1",
                "modelo-teste");

        service.registrar(
                caso,
                OperacaoGeracaoIa.GERAR_CASO,
                "caso-clinico-v2",
                "contexto clinico sensivel",
                java.util.Map.of("sintomas", "conteudo gerado sensivel"),
                "conteudo:42",
                1);

        ArgumentCaptor<AuditoriaGeracaoIa> captor =
                ArgumentCaptor.forClass(AuditoriaGeracaoIa.class);
        verify(repository).save(captor.capture());
        AuditoriaGeracaoIa auditoria = captor.getValue();
        assertThat(auditoria.getProvedor()).isEqualTo("gateway-interno");
        assertThat(auditoria.getModelo()).isEqualTo("modelo-teste");
        assertThat(auditoria.getHashContexto())
                .hasSize(64)
                .doesNotContain("sensivel");
        assertThat(auditoria.getHashSaida())
                .hasSize(64)
                .doesNotContain("sensivel");
        assertThat(auditoria.getReferenciaResultado()).isEqualTo("conteudo:42");
        assertThat(auditoria.getDadosDesidentificadosConfirmados()).isTrue();
    }
}
