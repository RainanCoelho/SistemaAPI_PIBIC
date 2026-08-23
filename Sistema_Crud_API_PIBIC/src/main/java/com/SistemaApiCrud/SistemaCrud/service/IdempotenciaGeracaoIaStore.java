package com.SistemaApiCrud.SistemaCrud.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.SistemaApiCrud.SistemaCrud.entity.SolicitacaoGeracaoIa;
import com.SistemaApiCrud.SistemaCrud.entity.Usuario;
import com.SistemaApiCrud.SistemaCrud.entity.enums.EstadoSolicitacaoGeracaoIa;
import com.SistemaApiCrud.SistemaCrud.entity.enums.OperacaoGeracaoIa;
import com.SistemaApiCrud.SistemaCrud.exception.RecursoNaoEncontradoException;
import com.SistemaApiCrud.SistemaCrud.repository.SolicitacaoGeracaoIaRepository;

@Service
public class IdempotenciaGeracaoIaStore {

    private final SolicitacaoGeracaoIaRepository repository;
    private final Duration ttl;

    public IdempotenciaGeracaoIaStore(
            SolicitacaoGeracaoIaRepository repository,
            @Value("${app.ia.idempotencia.ttl:PT1H}") Duration ttl) {
        this.repository = repository;
        this.ttl = ttl;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public InicioIdempotenciaIa iniciar(
            Usuario usuario,
            String chave,
            String hash,
            OperacaoGeracaoIa operacao,
            Long idCaso) {
        Instant agora = Instant.now();
        return repository.findByUsuarioIdAndChaveIdempotenciaForUpdate(usuario.getId(), chave)
                .map(solicitacao -> expirada(solicitacao, agora)
                        ? new InicioIdempotenciaIa(redefinir(solicitacao, hash, operacao, idCaso, agora), true)
                        : new InicioIdempotenciaIa(solicitacao, false))
                .orElseGet(() -> new InicioIdempotenciaIa(criar(usuario, chave, hash, operacao, idCaso), true));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public SolicitacaoGeracaoIa buscar(Long idUsuario, String chave) {
        return repository.findByUsuarioIdAndChaveIdempotencia(idUsuario, chave)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao idempotente nao encontrada"));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void concluirNaTransacaoAtual(Long id, int status, Collection<Long> idsResultado) {
        SolicitacaoGeracaoIa solicitacao = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao idempotente nao encontrada"));
        concluirSeEmAndamento(solicitacao, status, idsResultado);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void falhar(Long id) {
        SolicitacaoGeracaoIa solicitacao = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitacao idempotente nao encontrada"));
        if (solicitacao.getEstado() == EstadoSolicitacaoGeracaoIa.EM_ANDAMENTO) {
            solicitacao.setEstado(EstadoSolicitacaoGeracaoIa.FALHOU);
            solicitacao.setAtualizadoEm(Instant.now());
        }
    }

    private SolicitacaoGeracaoIa criar(
            Usuario usuario,
            String chave,
            String hash,
            OperacaoGeracaoIa operacao,
            Long idCaso) {
        Instant agora = Instant.now();
        SolicitacaoGeracaoIa solicitacao = new SolicitacaoGeracaoIa();
        solicitacao.setUsuario(usuario);
        solicitacao.setChaveIdempotencia(chave);
        solicitacao.setHashRequisicao(hash);
        solicitacao.setOperacao(operacao);
        solicitacao.setIdCaso(idCaso);
        solicitacao.setEstado(EstadoSolicitacaoGeracaoIa.EM_ANDAMENTO);
        solicitacao.setCriadoEm(agora);
        solicitacao.setAtualizadoEm(agora);
        solicitacao.setExpiraEm(agora.plus(ttl));
        return repository.saveAndFlush(solicitacao);
    }

    private SolicitacaoGeracaoIa redefinir(
            SolicitacaoGeracaoIa solicitacao,
            String hash,
            OperacaoGeracaoIa operacao,
            Long idCaso,
            Instant agora) {
        solicitacao.setHashRequisicao(hash);
        solicitacao.setOperacao(operacao);
        solicitacao.setIdCaso(idCaso);
        solicitacao.setEstado(EstadoSolicitacaoGeracaoIa.EM_ANDAMENTO);
        solicitacao.setStatusResposta(null);
        solicitacao.setIdsResultado(null);
        solicitacao.setAtualizadoEm(agora);
        solicitacao.setExpiraEm(agora.plus(ttl));
        return solicitacao;
    }

    private boolean expirada(SolicitacaoGeracaoIa solicitacao, Instant agora) {
        // Registros criados antes da V18 podem ainda nao ter expiracao preenchida.
        // Eles sao reinicializados sob lock, sem apagar o historico do ledger.
        return solicitacao.getExpiraEm() == null || !solicitacao.getExpiraEm().isAfter(agora);
    }

    private void concluirSeEmAndamento(
            SolicitacaoGeracaoIa solicitacao,
            int status,
            Collection<Long> idsResultado) {
        if (solicitacao.getEstado() != EstadoSolicitacaoGeracaoIa.EM_ANDAMENTO) {
            return;
        }
        solicitacao.setEstado(EstadoSolicitacaoGeracaoIa.CONCLUIDA);
        solicitacao.setStatusResposta(status);
        solicitacao.setIdsResultado(idsResultado.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")));
        solicitacao.setAtualizadoEm(Instant.now());
    }
}
