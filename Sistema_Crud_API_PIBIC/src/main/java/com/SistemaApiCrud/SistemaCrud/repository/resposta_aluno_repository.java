package com.SistemaApiCrud.SistemaCrud.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.SistemaApiCrud.SistemaCrud.entity.RespostaAluno;

import jakarta.persistence.LockModeType;

public interface resposta_aluno_repository extends JpaRepository<RespostaAluno, Long> {

    Page<RespostaAluno> findByAlunoIdAlunoOrderByDataRespostaDesc(Long idAluno, Pageable pageable);

    boolean existsByAlunoIdAlunoAndCasoClinicoIdCaso(Long idAluno, Long idCaso);

    long countByAlunoIdAluno(Long idAluno);

    long countByAlunoIdAlunoAndCorretaTrue(Long idAluno);

    @Query("""
            select count(resposta)
            from RespostaAluno resposta
            where resposta.aluno.idAluno = :idAluno
              and resposta.correta is not null
            """)
    long contarAvaliadasPorAluno(@Param("idAluno") Long idAluno);

    long countByCasoClinicoProfessorId(Long idProfessor);

    long countByCasoClinicoProfessorIdAndCorretaTrue(Long idProfessor);

    @Query("""
            select resposta
            from RespostaAluno resposta
            where resposta.casoClinico.idCaso = :idCaso
              and resposta.correta is null
            order by resposta.dataResposta asc
            """)
    Page<RespostaAluno> listarPendentesRevisaoPorCaso(
            @Param("idCaso") Long idCaso,
            Pageable paginacao);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select resposta from RespostaAluno resposta where resposta.id = :idResposta")
    Optional<RespostaAluno> buscarPorIdParaAtualizacao(
            @Param("idResposta") Long idResposta);

    @Query("""
            select count(resposta)
            from RespostaAluno resposta
            where resposta.casoClinico.professor.id = :idProfessor
              and resposta.correta is not null
            """)
    long contarAvaliadasPorProfessor(@Param("idProfessor") Long idProfessor);
}
