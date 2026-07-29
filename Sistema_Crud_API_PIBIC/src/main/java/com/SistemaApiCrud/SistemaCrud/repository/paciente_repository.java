package com.SistemaApiCrud.SistemaCrud.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.SistemaApiCrud.SistemaCrud.entity.paciente;

import jakarta.persistence.LockModeType;

public interface paciente_repository extends JpaRepository<paciente, Long> {

    List<paciente> findByCasoClinicoIdCaso(Long idCaso);

    List<paciente> findByCasoClinicoIdCasoOrderByIdPacienteAsc(Long idCaso);

    Optional<paciente> findFirstByCasoClinicoIdCasoOrderByIdPacienteAsc(Long idCaso);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select paciente from paciente paciente where paciente.idPaciente = :idPaciente")
    Optional<paciente> findByIdForUpdate(@Param("idPaciente") Long idPaciente);

    boolean existsByCasoClinicoIdCaso(Long idCaso);

    Page<paciente> findByCasoClinicoProfessorId(Long idProfessor, Pageable pageable);
}
