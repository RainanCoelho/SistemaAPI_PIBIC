package com.SistemaApiCrud.SistemaCrud.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.SistemaApiCrud.SistemaCrud.entity.RevisaoRespostaAluno;

public interface revisao_resposta_aluno_repository
        extends JpaRepository<RevisaoRespostaAluno, Long> {

    List<RevisaoRespostaAluno> findByRespostaIdOrderByVersaoRevisaoAsc(Long idResposta);

    Optional<RevisaoRespostaAluno> findFirstByRespostaIdOrderByVersaoRevisaoDesc(Long idResposta);
}
