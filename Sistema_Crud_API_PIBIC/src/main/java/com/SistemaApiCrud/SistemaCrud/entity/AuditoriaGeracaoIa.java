package com.SistemaApiCrud.SistemaCrud.entity;

import java.time.Instant;

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
@Table(name = "auditoria_geracao_ia")
public class AuditoriaGeracaoIa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_id_caso", nullable = false)
    private CasoClinico casoClinico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_id_usuario", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OperacaoGeracaoIa operacao;

    @Column(nullable = false, length = 150)
    private String provedor;

    @Column(nullable = false, length = 150)
    private String modelo;

    @Column(name = "versao_prompt", nullable = false, length = 40)
    private String versaoPrompt;

    @Column(name = "hash_contexto", nullable = false, length = 64)
    private String hashContexto;

    @Column(name = "hash_saida", nullable = false, length = 64)
    private String hashSaida;

    @Column(name = "referencia_resultado", nullable = false, columnDefinition = "TEXT")
    private String referenciaResultado;

    @Column(name = "quantidade_itens", nullable = false)
    private Integer quantidadeItens;

    @Column(name = "dados_desidentificados_confirmados", nullable = false)
    private Boolean dadosDesidentificadosConfirmados;

    @Column(name = "data_geracao", nullable = false)
    private Instant dataGeracao;
}
