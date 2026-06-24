package com.SistemaApiCrud.SistemaCrud.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "tentativa_caso",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tentativa_aluno_caso",
                columnNames = {"fk_id_aluno", "fk_id_caso"}))
public class TentativaCaso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "fk_id_aluno", nullable = false)
    private Aluno aluno;

    @ManyToOne
    @JoinColumn(name = "fk_id_caso", nullable = false)
    private casos_clinicos casoClinico;

    @Column(name = "data_inicio", nullable = false)
    private Instant dataInicio;

    @Column(name = "data_limite", nullable = false)
    private Instant dataLimite;

    @Column(name = "data_finalizacao")
    private Instant dataFinalizacao;
}
