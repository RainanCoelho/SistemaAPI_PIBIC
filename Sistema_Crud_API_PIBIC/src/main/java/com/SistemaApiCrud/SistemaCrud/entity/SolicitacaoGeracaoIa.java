package com.SistemaApiCrud.SistemaCrud.entity;

import java.time.Instant;

import com.SistemaApiCrud.SistemaCrud.entity.enums.EstadoSolicitacaoGeracaoIa;
import com.SistemaApiCrud.SistemaCrud.entity.enums.OperacaoGeracaoIa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "solicitacao_geracao_ia")
public class SolicitacaoGeracaoIa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fk_id_caso")
    private Long idCaso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OperacaoGeracaoIa operacao;

    @Column(name = "chave_idempotencia", nullable = false, length = 64)
    private String chaveIdempotencia;

    @Column(name = "hash_requisicao", nullable = false, length = 64)
    private String hashRequisicao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitacaoGeracaoIa estado;

    @Column(name = "status_resposta")
    private Integer statusResposta;

    @Column(name = "ids_resultado", length = 1000)
    private String idsResultado;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;
}
