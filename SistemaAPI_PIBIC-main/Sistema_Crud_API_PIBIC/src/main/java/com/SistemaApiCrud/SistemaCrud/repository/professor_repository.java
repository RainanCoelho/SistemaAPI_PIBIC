package com.SistemaApiCrud.SistemaCrud.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.SistemaApiCrud.SistemaCrud.entity.Professor;

public interface professor_repository extends JpaRepository<Professor, Long> {
	
}
