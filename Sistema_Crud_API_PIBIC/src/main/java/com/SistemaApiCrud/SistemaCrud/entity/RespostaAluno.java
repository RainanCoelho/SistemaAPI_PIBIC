package com.SistemaApiCrud.SistemaCrud.entity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "resposta_aluno")
public class RespostaAluno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_id_aluno", nullable = false)
    private Aluno aluno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_id_caso", nullable = false)
    private CasoClinico casoClinico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_id_pergunta", nullable = false)
    private Pergunta pergunta;

    @Column(name = "resposta_marcada", nullable = false, columnDefinition = "TEXT")
    private String respostaMarcada;

    @Column
    private Boolean correta;

    @Column(name = "data_resposta", nullable = false)
    private Instant dataResposta;

    @Version
    @Column(nullable = false)
    private Long versao = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_id_revisor")
    private Usuario revisor;

    @Column(name = "data_revisao")
    private Instant dataRevisao;

    @Column(name = "justificativa_revisao", columnDefinition = "TEXT")
    private String justificativaRevisao;

    @Column(name = "versao_revisao", nullable = false)
    private Long versaoRevisao = 0L;

    @PrePersist
    public void antesDeCriar() {
        if (dataResposta == null) {
            dataResposta = Instant.now().truncatedTo(ChronoUnit.MICROS);
        }
    }
}
