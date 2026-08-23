package com.SistemaApiCrud.SistemaCrud.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.SistemaApiCrud.SistemaCrud.entity.AuditoriaGeracaoIa;

public interface AuditoriaGeracaoIaRepository
        extends JpaRepository<AuditoriaGeracaoIa, Long> {

    Page<AuditoriaGeracaoIa> findByCasoClinicoIdCasoOrderByDataGeracaoDesc(
            Long idCaso,
            Pageable pageable);
}
