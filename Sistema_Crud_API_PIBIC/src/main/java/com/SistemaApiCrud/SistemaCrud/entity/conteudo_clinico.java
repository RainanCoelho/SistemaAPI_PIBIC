package com.SistemaApiCrud.SistemaCrud.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "conteudo_clinico")
public class conteudo_clinico {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_conteudo")
    private Long idConteudo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_id_caso", nullable = false)
    private casos_clinicos casoClinico;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sintomas;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contexto;

    @Column(name = "exam_clinico", nullable = false, columnDefinition = "TEXT")
    private String examClinico;

    @Column(name = "antec_clinico", nullable = false, columnDefinition = "TEXT")
    private String antecClinico;

    @Column(name = "diag_esperado", nullable = false, columnDefinition = "TEXT")
    private String diagEsperado;

    @Version
    @Column(nullable = false)
    private Long versao = 0L;
	
	
	
}
