package com.SistemaApiCrud.SistemaCrud.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.SistemaApiCrud.SistemaCrud.entity.paciente;

public interface paciente_repository extends JpaRepository<paciente, Long> {

    List<paciente> findByCasoClinicoIdCaso(Long idCaso);

    Page<paciente> findByCasoClinicoProfessorId(Long idProfessor, Pageable pageable);
}
