package com.SistemaApiCrud.SistemaCrud.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.SistemaApiCrud.SistemaCrud.entity.SolicitacaoGeracaoIa;

public interface SolicitacaoGeracaoIaRepository extends JpaRepository<SolicitacaoGeracaoIa, Long> {

    Optional<SolicitacaoGeracaoIa> findByUsuarioIdAndChaveIdempotencia(Long idUsuario, String chaveIdempotencia);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select solicitacao from SolicitacaoGeracaoIa solicitacao
            where solicitacao.usuario.id = :idUsuario
              and solicitacao.chaveIdempotencia = :chaveIdempotencia
            """)
    Optional<SolicitacaoGeracaoIa> findByUsuarioIdAndChaveIdempotenciaForUpdate(
            @Param("idUsuario") Long idUsuario,
            @Param("chaveIdempotencia") String chaveIdempotencia);
}
