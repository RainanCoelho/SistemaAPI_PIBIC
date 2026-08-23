package com.SistemaApiCrud.SistemaCrud.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.SistemaApiCrud.SistemaCrud.entity.Aluno;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {
}
