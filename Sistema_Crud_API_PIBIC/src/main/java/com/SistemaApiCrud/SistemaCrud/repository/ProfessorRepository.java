package com.SistemaApiCrud.SistemaCrud.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.SistemaApiCrud.SistemaCrud.entity.Professor;

public interface ProfessorRepository extends JpaRepository<Professor, Long> {
}
