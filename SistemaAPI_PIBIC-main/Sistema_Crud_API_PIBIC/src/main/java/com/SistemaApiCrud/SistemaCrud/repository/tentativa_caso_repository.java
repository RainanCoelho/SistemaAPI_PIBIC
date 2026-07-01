package com.SistemaApiCrud.SistemaCrud.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.SistemaApiCrud.SistemaCrud.entity.TentativaCaso;

public interface tentativa_caso_repository extends JpaRepository<TentativaCaso, Long> {

    Optional<TentativaCaso> findByAlunoIdAlunoAndCasoClinicoIdCaso(Long idAluno, Long idCaso);
}
