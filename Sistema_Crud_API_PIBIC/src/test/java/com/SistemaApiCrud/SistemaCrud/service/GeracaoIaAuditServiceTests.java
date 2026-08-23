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
import org.slf4j.MDC;

import com.SistemaApiCrud.SistemaCrud.entity.AuditoriaGeracaoIa;
import com.SistemaApiCrud.SistemaCrud.entity.Usuario;
import com.SistemaApiCrud.SistemaCrud.entity.CasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.enums.OperacaoGeracaoIa;
import com.SistemaApiCrud.SistemaCrud.repository.AuditoriaGeracaoIaRepository;
import com.SistemaApiCrud.SistemaCrud.repository.UsuarioRepository;

class GeracaoIaAuditServiceTests {

    private final AuditoriaGeracaoIaRepository repository =
            mock(AuditoriaGeracaoIaRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);

    @AfterEach
    void limparAutenticacao() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void deveRegistrarSomenteHashesEMetadadosDeProveniencia() {
        Usuario usuario = new Usuario();
        usuario.setId(9L);
        usuario.setUsername("professor-auditor");
        CasoClinico caso = new CasoClinico();
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

    @Test
    void deveRegistrarMetricasSemPersistirConteudoClinico() {
        Usuario usuario = new Usuario();
        usuario.setId(9L);
        usuario.setUsername("professor-auditor");
        CasoClinico caso = new CasoClinico();
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
        MDC.put("correlationId", "correlacao-teste");
        GeracaoIaAuditService service = new GeracaoIaAuditService(
                repository,
                usuarioRepository,
                "http://gateway-interno:3001/v1",
                "modelo-configurado");

        service.registrar(
                caso,
                OperacaoGeracaoIa.GERAR_CASO,
                "caso-clinico-v2",
                "contexto clinico sensivel",
                java.util.Map.of("resultado", "conteudo gerado sensivel"),
                "conteudo:42",
                1,
                new RespostaIaComMetricas<>(
                        new Object(),
                        1250L,
                        "modelo-efetivo",
                        120,
                        80));

        ArgumentCaptor<AuditoriaGeracaoIa> captor =
                ArgumentCaptor.forClass(AuditoriaGeracaoIa.class);
        verify(repository).save(captor.capture());
        AuditoriaGeracaoIa auditoria = captor.getValue();
        assertThat(auditoria.getModelo()).isEqualTo("modelo-efetivo");
        assertThat(auditoria.getDuracaoProvedorMs()).isEqualTo(1250L);
        assertThat(auditoria.getTokensEntrada()).isEqualTo(120);
        assertThat(auditoria.getTokensSaida()).isEqualTo(80);
        assertThat(auditoria.getCorrelationId()).isEqualTo("correlacao-teste");
    }
}
