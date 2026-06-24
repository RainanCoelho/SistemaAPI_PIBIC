package com.SistemaApiCrud.SistemaCrud.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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

	    @ManyToOne
	    @JoinColumn(name = "fk_id_caso", nullable = false)
	    private casos_clinicos casoClinico;

        @Column(nullable = false, columnDefinition = "TEXT")
	    private String texto;

	    @Column(name = "alternativa_a", columnDefinition = "TEXT")
	    private String alternativaA;

	    @Column(name = "alternativa_b", columnDefinition = "TEXT")
	    private String alternativaB;

	    @Column(name = "alternativa_c", columnDefinition = "TEXT")
	    private String alternativaC;

	    @Column(name = "alternativa_d", columnDefinition = "TEXT")
	    private String alternativaD;

	    @Column(name = "alternativa_e", columnDefinition = "TEXT")
	    private String alternativaE;
	    
        @Column(nullable = false, columnDefinition = "TEXT")
	    private String resposta;

	    @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 40)
	    private TipoPergunta tipo;

        @Column(nullable = false, columnDefinition = "TEXT")
	    private String gabarito;

}
