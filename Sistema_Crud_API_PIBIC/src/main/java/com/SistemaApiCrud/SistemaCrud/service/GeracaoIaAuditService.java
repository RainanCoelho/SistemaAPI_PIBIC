package com.SistemaApiCrud.SistemaCrud.service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.SistemaApiCrud.SistemaCrud.dto.AuditoriaGeracaoIaDTO;
import com.SistemaApiCrud.SistemaCrud.entity.AuditoriaGeracaoIa;
import com.SistemaApiCrud.SistemaCrud.entity.Usuario;
import com.SistemaApiCrud.SistemaCrud.entity.CasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.enums.OperacaoGeracaoIa;
import com.SistemaApiCrud.SistemaCrud.repository.AuditoriaGeracaoIaRepository;
import com.SistemaApiCrud.SistemaCrud.repository.UsuarioRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeracaoIaAuditService {

    private static final Logger LOG = LoggerFactory.getLogger(GeracaoIaAuditService.class);

    private final AuditoriaGeracaoIaRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final String provedor;
    private final String modelo;

    public GeracaoIaAuditService(
            AuditoriaGeracaoIaRepository repository,
            UsuarioRepository usuarioRepository,
            @Value("${spring.ai.openai.base-url:nao-configurado}") String urlBase,
            @Value("${spring.ai.openai.chat.model:nao-configurado}") String modelo) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.provedor = identificarProvedor(urlBase);
        this.modelo = limitar(modelo, 150);
    }

    @Transactional
    public void registrar(
            CasoClinico caso,
            OperacaoGeracaoIa operacao,
            String versaoPrompt,
            String contexto,
            Object saida,
            String referenciaResultado,
            int quantidadeItens) {
        registrar(
                caso,
                operacao,
                versaoPrompt,
                contexto,
                saida,
                referenciaResultado,
                quantidadeItens,
                null);
    }

    @Transactional
    public void registrar(
            CasoClinico caso,
            OperacaoGeracaoIa operacao,
            String versaoPrompt,
            String contexto,
            Object saida,
            String referenciaResultado,
            int quantidadeItens,
            RespostaIaComMetricas<?> metricas) {
        Usuario usuario = usuarioAutenticado();
        AuditoriaGeracaoIa auditoria = new AuditoriaGeracaoIa();
        auditoria.setCasoClinico(caso);
        auditoria.setUsuario(usuario);
        auditoria.setOperacao(operacao);
        auditoria.setProvedor(provedor);
        auditoria.setModelo(modeloEfetivo(metricas));
        auditoria.setVersaoPrompt(versaoPrompt);
        auditoria.setHashContexto(calcularHash(contexto));
        auditoria.setHashSaida(calcularHash(serializar(saida)));
        auditoria.setReferenciaResultado(referenciaResultado);
        auditoria.setQuantidadeItens(quantidadeItens);
        auditoria.setDadosDesidentificadosConfirmados(true);
        auditoria.setDuracaoProvedorMs(duracao(metricas));
        auditoria.setTokensEntrada(tokensEntrada(metricas));
        auditoria.setTokensSaida(tokensSaida(metricas));
        auditoria.setCorrelationId(correlationId());
        auditoria.setDataGeracao(Instant.now().truncatedTo(ChronoUnit.MICROS));
        repository.save(auditoria);
        LOG.info(
                "geracao_ia_concluida operacao={} casoId={} correlationId={} duracaoProvedorMs={} modelo={} tokensEntrada={} tokensSaida={}",
                operacao,
                caso.getIdCaso(),
                auditoria.getCorrelationId(),
                auditoria.getDuracaoProvedorMs(),
                auditoria.getModelo(),
                auditoria.getTokensEntrada(),
                auditoria.getTokensSaida());
    }

    @Transactional(readOnly = true)
    public Page<AuditoriaGeracaoIaDTO> listarPorCaso(Long idCaso, Pageable pageable) {
        return repository.findByCasoClinicoIdCasoOrderByDataGeracaoDesc(idCaso, pageable)
                .map(this::paraDTO);
    }

    private Usuario usuarioAutenticado() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null
                || !autenticacao.isAuthenticated()
                || autenticacao.getName() == null
                || autenticacao.getName().isBlank()) {
            throw new IllegalStateException("Nao foi possivel identificar o usuario da geracao de IA");
        }
        return usuarioRepository.findByUsername(autenticacao.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "O usuario autenticado da geracao de IA nao foi encontrado"));
    }

    private String serializar(Object saida) {
        try {
            return objectMapper.writeValueAsString(saida);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Nao foi possivel registrar a proveniencia da saida de IA", ex);
        }
    }

    private String calcularHash(String valor) {
        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256")
                    .digest((valor == null ? "" : valor).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumo);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponivel", ex);
        }
    }

    private String identificarProvedor(String urlBase) {
        try {
            String host = URI.create(urlBase).getHost();
            return host == null || host.isBlank() ? "nao-identificado" : limitar(host, 150);
        } catch (IllegalArgumentException ex) {
            return "nao-identificado";
        }
    }

    private String limitar(String valor, int limite) {
        String normalizado = valor == null || valor.isBlank() ? "nao-configurado" : valor.trim();
        return normalizado.substring(0, Math.min(normalizado.length(), limite));
    }

    private AuditoriaGeracaoIaDTO paraDTO(AuditoriaGeracaoIa auditoria) {
        return new AuditoriaGeracaoIaDTO(
                auditoria.getId(),
                auditoria.getCasoClinico().getIdCaso(),
                auditoria.getUsuario().getId(),
                auditoria.getOperacao(),
                auditoria.getProvedor(),
                auditoria.getModelo(),
                auditoria.getVersaoPrompt(),
                auditoria.getHashContexto(),
                auditoria.getHashSaida(),
                auditoria.getReferenciaResultado(),
                auditoria.getQuantidadeItens(),
                auditoria.getDadosDesidentificadosConfirmados(),
                auditoria.getDuracaoProvedorMs(),
                auditoria.getTokensEntrada(),
                auditoria.getTokensSaida(),
                auditoria.getCorrelationId(),
                auditoria.getDataGeracao());
    }

    private String modeloEfetivo(RespostaIaComMetricas<?> metricas) {
        if (metricas == null || metricas.modeloEfetivo() == null || metricas.modeloEfetivo().isBlank()) {
            return modelo;
        }
        return limitar(metricas.modeloEfetivo(), 150);
    }

    private Long duracao(RespostaIaComMetricas<?> metricas) {
        return metricas == null || metricas.duracaoProvedorMs() <= 0
                ? null
                : metricas.duracaoProvedorMs();
    }

    private Integer tokensEntrada(RespostaIaComMetricas<?> metricas) {
        return metricas == null ? null : metricas.tokensEntrada();
    }

    private Integer tokensSaida(RespostaIaComMetricas<?> metricas) {
        return metricas == null ? null : metricas.tokensSaida();
    }

    private String correlationId() {
        String valor = MDC.get("correlationId");
        return valor == null || valor.isBlank() ? null : limitar(valor, 64);
    }
}
