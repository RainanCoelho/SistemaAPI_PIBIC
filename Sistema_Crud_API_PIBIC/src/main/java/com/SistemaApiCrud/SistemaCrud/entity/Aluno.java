package com.SistemaApiCrud.SistemaCrud.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
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
@Table(name = "aluno")
public class Aluno {

		@Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
		@Column(name = "id_aluno")
	    private Long idAluno;

	    @Column(nullable = false, length = 150)
	    private String nome;

	    @Column(nullable = false, length = 254)
	    private String email;

	    @Column(nullable = false, length = 120)
	    private String curso;

	    @Column(nullable = false, length = 20)
	    private String periodo;
	
	    
	
	
}
