package com.SistemaApiCrud.SistemaCrud.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.SistemaApiCrud.SistemaCrud.entity.ConteudoClinico;

import jakarta.persistence.LockModeType;

public interface ConteudoClinicoRepository extends JpaRepository<ConteudoClinico, Long> {

    List<ConteudoClinico> findByCasoClinicoIdCaso(Long idCaso);

    boolean existsByCasoClinicoIdCaso(Long idCaso);

    Optional<ConteudoClinico> findFirstByCasoClinicoIdCasoOrderByIdConteudoDesc(Long idCaso);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select conteudo from ConteudoClinico conteudo where conteudo.idConteudo = :idConteudo")
    Optional<ConteudoClinico> findByIdForUpdate(@Param("idConteudo") Long idConteudo);

    Page<ConteudoClinico> findByCasoClinicoProfessorId(Long idProfessor, Pageable pageable);
}
