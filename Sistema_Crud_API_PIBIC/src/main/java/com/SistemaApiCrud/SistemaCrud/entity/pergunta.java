package com.SistemaApiCrud.SistemaCrud.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.SistemaApiCrud.SistemaCrud.entity.enums.TipoPergunta;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "pergunta")
public class pergunta {

	 	@Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    @ManyToOne(fetch = FetchType.LAZY, optional = false)
	    @JoinColumn(name = "fk_id_caso", nullable = false)
	    private casos_clinicos casoClinico;

        @Column(nullable = false, columnDefinition = "TEXT")
	    private String texto;

        @Column(nullable = false, columnDefinition = "TEXT")
	    private String resposta;

	    @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 40)
	    private TipoPergunta tipo;

        @Column(nullable = false, columnDefinition = "TEXT")
	    private String gabarito;

        @Version
        @Column(nullable = false)
        private Long versao = 0L;

}
