package com.SistemaApiCrud.SistemaCrud.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.SistemaApiCrud.SistemaCrud.entity.casos_clinicos;
import com.SistemaApiCrud.SistemaCrud.entity.enums.StatusCasoClinico;

import jakarta.persistence.LockModeType;

public interface caso_clinico_repository extends JpaRepository<casos_clinicos, Long>, JpaSpecificationExecutor<casos_clinicos> {

    Page<casos_clinicos> findByProfessorId(Long idProfessor, Pageable pageable);

    Page<casos_clinicos> findByStatus(StatusCasoClinico status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select caso from casos_clinicos caso where caso.idCaso = :idCaso")
    java.util.Optional<casos_clinicos> findByIdForUpdate(@Param("idCaso") Long idCaso);
}
