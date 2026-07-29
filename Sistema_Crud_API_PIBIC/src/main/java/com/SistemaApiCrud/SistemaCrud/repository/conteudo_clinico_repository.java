package com.SistemaApiCrud.SistemaCrud.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.SistemaApiCrud.SistemaCrud.entity.conteudo_clinico;

import jakarta.persistence.LockModeType;

public interface conteudo_clinico_repository extends JpaRepository<conteudo_clinico, Long> {

    List<conteudo_clinico> findByCasoClinicoIdCaso(Long idCaso);

    boolean existsByCasoClinicoIdCaso(Long idCaso);

    Optional<conteudo_clinico> findFirstByCasoClinicoIdCasoOrderByIdConteudoDesc(Long idCaso);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select conteudo from conteudo_clinico conteudo where conteudo.idConteudo = :idConteudo")
    Optional<conteudo_clinico> findByIdForUpdate(@Param("idConteudo") Long idConteudo);

    Page<conteudo_clinico> findByCasoClinicoProfessorId(Long idProfessor, Pageable pageable);
}
