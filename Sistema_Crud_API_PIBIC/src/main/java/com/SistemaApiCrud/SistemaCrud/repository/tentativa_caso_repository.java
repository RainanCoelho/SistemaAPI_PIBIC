package com.SistemaApiCrud.SistemaCrud.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.SistemaApiCrud.SistemaCrud.entity.TentativaCaso;

import jakarta.persistence.LockModeType;

public interface tentativa_caso_repository extends JpaRepository<TentativaCaso, Long> {

    Optional<TentativaCaso> findByAlunoIdAlunoAndCasoClinicoIdCaso(Long idAluno, Long idCaso);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select tentativa
            from TentativaCaso tentativa
            where tentativa.aluno.idAluno = :idAluno
              and tentativa.casoClinico.idCaso = :idCaso
            """)
    Optional<TentativaCaso> findByAlunoECasoForUpdate(
            @Param("idAluno") Long idAluno,
            @Param("idCaso") Long idCaso);
}
