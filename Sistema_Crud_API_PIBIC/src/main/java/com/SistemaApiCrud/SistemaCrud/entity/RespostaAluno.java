package com.SistemaApiCrud.SistemaCrud.entity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @ManyToOne
    @JoinColumn(name = "fk_id_aluno", nullable = false)
    private Aluno aluno;

    @ManyToOne
    @JoinColumn(name = "fk_id_caso", nullable = false)
    private casos_clinicos casoClinico;

    @ManyToOne
    @JoinColumn(name = "fk_id_pergunta", nullable = false)
    private pergunta pergunta;

    @Column(name = "resposta_marcada", nullable = false, columnDefinition = "TEXT")
    private String respostaMarcada;

    @Column
    private Boolean correta;

    @Column(name = "data_resposta", nullable = false)
    private Instant dataResposta;

    @Version
    @Column(nullable = false)
    private Long versao = 0L;

    @PrePersist
    public void antesDeCriar() {
        if (dataResposta == null) {
            dataResposta = Instant.now().truncatedTo(ChronoUnit.MICROS);
        }
    }
}
