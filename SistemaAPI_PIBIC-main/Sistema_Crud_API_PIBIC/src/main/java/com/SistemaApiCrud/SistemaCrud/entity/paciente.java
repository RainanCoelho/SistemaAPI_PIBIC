package com.SistemaApiCrud.SistemaCrud.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.SistemaApiCrud.SistemaCrud.entity.enums.EstadoCivil;
import com.SistemaApiCrud.SistemaCrud.entity.enums.Sexo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "paciente")
public class paciente {

	  @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    @Column(name = "id_paciente")
	    private Long idPaciente;

	    @ManyToOne
	    @JoinColumn(name = "fk_id_caso", nullable = false)
	    private casos_clinicos casoClinico;

        @Column(nullable = false, length = 150)
	    private String nome;

        @Column(nullable = false, length = 120)
	    private String profissao;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 30)
	    private Sexo sexo;

        @Column(nullable = false)
	    private Integer idade;

        @Enumerated(EnumType.STRING)
	    @Column(name = "estado_civil", nullable = false, length = 30)
	    private EstadoCivil estadoCivil;

        @Column(nullable = false, length = 20)
	    private String altura;

        @Column(nullable = false, length = 20)
	    private String peso;
	
	
	
	
}
