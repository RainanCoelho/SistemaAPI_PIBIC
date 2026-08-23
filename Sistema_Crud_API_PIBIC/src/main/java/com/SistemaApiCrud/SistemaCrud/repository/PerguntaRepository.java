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

import com.SistemaApiCrud.SistemaCrud.entity.Pergunta;

import jakarta.persistence.LockModeType;

public interface PerguntaRepository extends JpaRepository<Pergunta, Long> {

    List<Pergunta> findByCasoClinicoIdCaso(Long idCaso);

    List<Pergunta> findByCasoClinicoIdCasoAndIdIn(
            Long idCaso,
            Collection<Long> idsPerguntas);

    boolean existsByCasoClinicoIdCaso(Long idCaso);

    long countByCasoClinicoIdCaso(Long idCaso);

    @Query("select pergunta.texto from Pergunta pergunta where pergunta.casoClinico.idCaso = :idCaso")
    List<String> findTextosByCasoClinicoIdCaso(@Param("idCaso") Long idCaso);

    Page<Pergunta> findByCasoClinicoIdCaso(Long idCaso, Pageable pageable);

    Page<Pergunta> findByCasoClinicoProfessorId(Long idProfessor, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pergunta from Pergunta pergunta where pergunta.id = :idPergunta")
    Optional<Pergunta> findByIdForUpdate(@Param("idPergunta") Long idPergunta);
}
