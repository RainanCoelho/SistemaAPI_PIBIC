package com.SistemaApiCrud.SistemaCrud.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.SistemaApiCrud.SistemaCrud.entity.CasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.enums.StatusCasoClinico;

import jakarta.persistence.LockModeType;

public interface CasoClinicoRepository extends JpaRepository<CasoClinico, Long>, JpaSpecificationExecutor<CasoClinico> {

    Page<CasoClinico> findByProfessorId(Long idProfessor, Pageable pageable);

    Page<CasoClinico> findByStatus(StatusCasoClinico status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select caso from CasoClinico caso where caso.idCaso = :idCaso")
    java.util.Optional<CasoClinico> findByIdForUpdate(@Param("idCaso") Long idCaso);
}
