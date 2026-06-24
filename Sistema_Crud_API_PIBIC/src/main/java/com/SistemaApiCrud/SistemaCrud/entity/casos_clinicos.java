package com.SistemaApiCrud.SistemaCrud.entity;

import java.time.LocalDateTime;

import com.SistemaApiCrud.SistemaCrud.entity.enums.NivelDificuldade;
import com.SistemaApiCrud.SistemaCrud.entity.enums.StatusCasoClinico;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "casos_clinicos")
public class casos_clinicos {

    public static final int TEMPO_LIMITE_PADRAO_MINUTOS = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_caso")
    private Long idCaso;

    @ManyToOne
    @JoinColumn(name = "fk_id_professor", nullable = false)
    private Professor professor;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, length = 30)
    private String dificuldade;

    @Column(nullable = false, length = 120)
    private String disciplina;

    @Column(name = "area_saude", nullable = false, length = 120)
    private String areaSaude;

    @Column(nullable = false, length = 60)
    private String estilo;

    @Column(nullable = false, length = 120)
    private String especialidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusCasoClinico status;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao", nullable = false)
    private LocalDateTime dataAtualizacao;

    @Column(name = "objetivo_aprendizagem", columnDefinition = "TEXT")
    private String objetivoAprendizagem;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_dificuldade", length = 30)
    private NivelDificuldade nivelDificuldade;

    @Column(name = "tempo_limite_minutos", nullable = false)
    private Integer tempoLimiteMinutos = TEMPO_LIMITE_PADRAO_MINUTOS;

    @PrePersist
    public void antesDeCriar() {
        LocalDateTime agora = LocalDateTime.now();
        dataCriacao = agora;
        dataAtualizacao = agora;

        if (status == null) {
            status = StatusCasoClinico.RASCUNHO;
        }
        if (tempoLimiteMinutos == null) {
            tempoLimiteMinutos = TEMPO_LIMITE_PADRAO_MINUTOS;
        }
    }

    @PreUpdate
    public void antesDeAtualizar() {
        dataAtualizacao = LocalDateTime.now();
    }
}
