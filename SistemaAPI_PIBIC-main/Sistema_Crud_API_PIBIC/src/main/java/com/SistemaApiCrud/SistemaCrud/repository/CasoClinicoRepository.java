package com.SistemaApiCrud.SistemaCrud.repository;

import com.SistemaApiCrud.SistemaCrud.entity.casos_clinicos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CasoClinicoRepository extends JpaRepository<casos_clinicos, Long> {




}
