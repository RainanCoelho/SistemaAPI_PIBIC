package com.SistemaApiCrud.SistemaCrud.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.SistemaApiCrud.SistemaCrud.entity.pergunta;

public interface pergunta_repository extends JpaRepository<pergunta, Long> {

    List<pergunta> findByCasoClinicoIdCaso(Long idCaso);

    List<pergunta> findByCasoClinicoProfessorId(Long idProfessor);

    Page<pergunta> findByCasoClinicoIdCaso(Long idCaso, Pageable pageable);

    Page<pergunta> findByCasoClinicoProfessorId(Long idProfessor, Pageable pageable);
}
