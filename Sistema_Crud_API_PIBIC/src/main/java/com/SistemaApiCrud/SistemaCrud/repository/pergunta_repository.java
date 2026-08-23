package com.SistemaApiCrud.SistemaCrud.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.SistemaApiCrud.SistemaCrud.entity.pergunta;

import jakarta.persistence.LockModeType;

public interface pergunta_repository extends JpaRepository<pergunta, Long> {

    List<pergunta> findByCasoClinicoIdCaso(Long idCaso);

    List<pergunta> findByCasoClinicoIdCasoAndIdIn(
            Long idCaso,
            Collection<Long> idsPerguntas);

    boolean existsByCasoClinicoIdCaso(Long idCaso);

    long countByCasoClinicoIdCaso(Long idCaso);

    @Query("select pergunta.texto from pergunta pergunta where pergunta.casoClinico.idCaso = :idCaso")
    List<String> findTextosByCasoClinicoIdCaso(@Param("idCaso") Long idCaso);

    Page<pergunta> findByCasoClinicoIdCaso(Long idCaso, Pageable pageable);

    Page<pergunta> findByCasoClinicoProfessorId(Long idProfessor, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pergunta from pergunta pergunta where pergunta.id = :idPergunta")
    Optional<pergunta> findByIdForUpdate(@Param("idPergunta") Long idPergunta);
}
