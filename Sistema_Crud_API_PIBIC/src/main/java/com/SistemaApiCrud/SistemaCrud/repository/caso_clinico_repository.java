package com.SistemaApiCrud.SistemaCrud.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.SistemaApiCrud.SistemaCrud.entity.casos_clinicos;
import com.SistemaApiCrud.SistemaCrud.entity.enums.StatusCasoClinico;

public interface caso_clinico_repository extends JpaRepository<casos_clinicos, Long>, JpaSpecificationExecutor<casos_clinicos> {

    List<casos_clinicos> findByProfessorId(Long idProfessor);

    List<casos_clinicos> findByStatus(StatusCasoClinico status);

    Page<casos_clinicos> findByProfessorId(Long idProfessor, Pageable pageable);

    Page<casos_clinicos> findByStatus(StatusCasoClinico status, Pageable pageable);
}
