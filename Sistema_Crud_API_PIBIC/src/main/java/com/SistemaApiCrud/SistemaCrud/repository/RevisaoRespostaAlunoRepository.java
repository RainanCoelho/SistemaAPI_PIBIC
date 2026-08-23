package com.SistemaApiCrud.SistemaCrud.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.SistemaApiCrud.SistemaCrud.entity.RevisaoRespostaAluno;

public interface RevisaoRespostaAlunoRepository
        extends JpaRepository<RevisaoRespostaAluno, Long> {

    List<RevisaoRespostaAluno> findByRespostaIdOrderByVersaoRevisaoAsc(Long idResposta);

    Page<RevisaoRespostaAluno> findByRespostaIdOrderByVersaoRevisaoAsc(
            Long idResposta,
            Pageable paginacao);

    Optional<RevisaoRespostaAluno> findFirstByRespostaIdOrderByVersaoRevisaoDesc(Long idResposta);
}
