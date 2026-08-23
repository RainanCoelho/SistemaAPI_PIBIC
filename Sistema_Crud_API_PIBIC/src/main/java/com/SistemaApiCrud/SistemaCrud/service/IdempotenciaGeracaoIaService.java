package com.SistemaApiCrud.SistemaCrud.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.SistemaApiCrud.SistemaCrud.entity.SolicitacaoGeracaoIa;
import com.SistemaApiCrud.SistemaCrud.entity.Usuario;
import com.SistemaApiCrud.SistemaCrud.entity.enums.EstadoSolicitacaoGeracaoIa;
import com.SistemaApiCrud.SistemaCrud.entity.enums.OperacaoGeracaoIa;
import com.SistemaApiCrud.SistemaCrud.exception.BadRequestException;
import com.SistemaApiCrud.SistemaCrud.exception.ConflitoIdempotenciaException;
import com.SistemaApiCrud.SistemaCrud.exception.SolicitacaoIaEmAndamentoException;
import com.SistemaApiCrud.SistemaCrud.repository.UsuarioRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class IdempotenciaGeracaoIaService {

    private static final long RETRY_AFTER_SECONDS = 2;
    private static final Logger LOG = LoggerFactory.getLogger(IdempotenciaGeracaoIaService.class);

    private final IdempotenciaGeracaoIaStore store;
    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public IdempotenciaGeracaoIaService(
            IdempotenciaGeracaoIaStore store,
            UsuarioRepository usuarioRepository) {
        this.store = store;
        this.usuarioRepository = usuarioRepository;
    }

    public <T> RespostaIdempotente<T> executar(
            String chaveRecebida,
            OperacaoGeracaoIa operacao,
            Long idCaso,
            Object requisicao,
            int statusDeSucesso,
            Function<SolicitacaoGeracaoIa, T> reconstruirResposta,
            Supplier<T> operacaoIa) {
        if (chaveRecebida == null) {
            return new RespostaIdempotente<>(operacaoIa.get(), statusDeSucesso);
        }

        String chave = validarChave(chaveRecebida);
        Usuario usuario = usuarioAutenticado();
        String hash = hashDaRequisicao(operacao, idCaso, requisicao);
        InicioIdempotenciaIa inicio = iniciarOuBuscar(usuario, chave, hash, operacao, idCaso);
        SolicitacaoGeracaoIa solicitacao = inicio.solicitacao();
        if (solicitacao.getId() == null) {
            throw new IllegalStateException("Solicitacao de idempotencia sem identificador");
        }

        if (!hash.equals(solicitacao.getHashRequisicao())
                || solicitacao.getOperacao() != operacao
                || !idCaso.equals(solicitacao.getIdCaso())) {
            throw new ConflitoIdempotenciaException();
        }
        if (solicitacao.getEstado() == EstadoSolicitacaoGeracaoIa.CONCLUIDA) {
            return new RespostaIdempotente<>(reconstruirResposta.apply(solicitacao), solicitacao.getStatusResposta());
        }
        if (solicitacao.getEstado() == EstadoSolicitacaoGeracaoIa.EM_ANDAMENTO && !inicio.criada()) {
            throw new SolicitacaoIaEmAndamentoException(RETRY_AFTER_SECONDS);
        }
        if (solicitacao.getEstado() == EstadoSolicitacaoGeracaoIa.FALHOU) {
            throw new ConflitoIdempotenciaException();
        }

        try {
            T resposta = ContextoIdempotenciaGeracaoIa.executar(solicitacao.getId(), operacaoIa);
            return new RespostaIdempotente<>(resposta, statusDeSucesso);
        } catch (RuntimeException ex) {
            SolicitacaoGeracaoIa estadoAtual = buscarSemMascarar(usuario.getId(), chave);
            if (estadoAtual != null && estadoAtual.getEstado() == EstadoSolicitacaoGeracaoIa.CONCLUIDA) {
                LOG.warn(
                        "Falha apos a conclusao atomica da solicitacao idempotente {}. Mantendo sucesso.",
                        solicitacao.getId());
                return new RespostaIdempotente<>(
                        reconstruirResposta.apply(estadoAtual),
                        estadoAtual.getStatusResposta());
            }
            try {
                store.falhar(solicitacao.getId());
            } catch (RuntimeException falhaNoRegistro) {
                ex.addSuppressed(falhaNoRegistro);
            }
            throw ex;
        }
    }

    private SolicitacaoGeracaoIa buscarSemMascarar(Long idUsuario, String chave) {
        try {
            return store.buscar(idUsuario, chave);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private InicioIdempotenciaIa iniciarOuBuscar(
            Usuario usuario,
            String chave,
            String hash,
            OperacaoGeracaoIa operacao,
            Long idCaso) {
        try {
            return store.iniciar(usuario, chave, hash, operacao, idCaso);
        } catch (DataIntegrityViolationException ex) {
            return new InicioIdempotenciaIa(store.buscar(usuario.getId(), chave), false);
        }
    }

    private String validarChave(String chave) {
        if (chave.isBlank() || chave.length() > 64) {
            throw new BadRequestException("Idempotency-Key deve ser um UUID valido");
        }
        try {
            return UUID.fromString(chave.trim()).toString();
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Idempotency-Key deve ser um UUID valido");
        }
    }

    private Usuario usuarioAutenticado() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated()) {
            throw new BadRequestException("Nao foi possivel identificar o usuario da solicitacao");
        }
        return usuarioRepository.findByUsername(autenticacao.getName())
                .orElseThrow(() -> new BadRequestException(
                        "Nao foi possivel identificar o usuario da solicitacao"));
    }

    private String hashDaRequisicao(OperacaoGeracaoIa operacao, Long idCaso, Object requisicao) {
        try {
            String serializada = objectMapper.writeValueAsString(requisicao);
            return calcularHash(operacao.name() + "|" + idCaso + "|" + serializada);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Nao foi possivel validar a requisicao idempotente");
        }
    }

    private String calcularHash(String valor) {
        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumo);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponivel", ex);
        }
    }
}
