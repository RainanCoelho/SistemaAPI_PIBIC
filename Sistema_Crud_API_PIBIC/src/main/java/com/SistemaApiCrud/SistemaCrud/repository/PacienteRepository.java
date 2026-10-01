package com.SistemaApiCrud.SistemaCrud.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.SistemaApiCrud.SistemaCrud.entity.Paciente;

import jakarta.persistence.LockModeType;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    List<Paciente> findByCasoClinicoIdCaso(Long idCaso);

    List<Paciente> findByCasoClinicoIdCasoOrderByIdPacienteAsc(Long idCaso);

    Optional<Paciente> findFirstByCasoClinicoIdCasoOrderByIdPacienteAsc(Long idCaso);

    @Query("select item.casoClinico.idCaso from Paciente item where item.idPaciente = :id")
    Optional<Long> findIdCasoById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select paciente from Paciente paciente where paciente.idPaciente = :idPaciente")
    Optional<Paciente> findByIdForUpdate(@Param("idPaciente") Long idPaciente);

    boolean existsByCasoClinicoIdCaso(Long idCaso);

    Page<Paciente> findByCasoClinicoProfessorId(Long idProfessor, Pageable pageable);
}
