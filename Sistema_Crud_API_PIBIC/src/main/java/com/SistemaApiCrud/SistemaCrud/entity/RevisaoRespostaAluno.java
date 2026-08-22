package com.SistemaApiCrud.SistemaCrud.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "revisao_resposta_aluno")
public class RevisaoRespostaAluno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_id_resposta", nullable = false)
    private RespostaAluno resposta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_id_revisor", nullable = false)
    private Usuario revisor;

    @Column(nullable = false)
    private Boolean correta;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String justificativa;

    @Column(name = "data_revisao", nullable = false)
    private Instant dataRevisao;

    @Column(name = "versao_revisao", nullable = false)
    private Long versaoRevisao;
}
