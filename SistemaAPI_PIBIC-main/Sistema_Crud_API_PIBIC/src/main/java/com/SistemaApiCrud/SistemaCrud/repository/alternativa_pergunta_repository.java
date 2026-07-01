package com.SistemaApiCrud.SistemaCrud.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.SistemaApiCrud.SistemaCrud.entity.AlternativaPergunta;

public interface alternativa_pergunta_repository extends JpaRepository<AlternativaPergunta, Long> {

    List<AlternativaPergunta> findByPerguntaIdOrderByLetra(Long idPergunta);

    void deleteByPerguntaId(Long idPergunta);
}
