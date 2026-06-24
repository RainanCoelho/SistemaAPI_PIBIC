package com.SistemaApiCrud.SistemaCrud.entity;

import com.SistemaApiCrud.SistemaCrud.entity.enums.PapelUsuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, length = 255)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PapelUsuario role;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "versao_credencial", nullable = false)
    private Long versaoCredencial = 0L;

    @OneToOne
    @JoinColumn(name = "fk_id_aluno", unique = true)
    private Aluno aluno;

    @OneToOne
    @JoinColumn(name = "fk_id_professor", unique = true)
    private Professor professor;
}
