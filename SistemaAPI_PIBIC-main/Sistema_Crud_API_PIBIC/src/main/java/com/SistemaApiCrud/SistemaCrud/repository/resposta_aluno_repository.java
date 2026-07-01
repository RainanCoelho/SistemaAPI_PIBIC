package com.SistemaApiCrud.SistemaCrud.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.SistemaApiCrud.SistemaCrud.entity.RespostaAluno;

public interface resposta_aluno_repository extends JpaRepository<RespostaAluno, Long> {

    List<RespostaAluno> findByAlunoIdAlunoOrderByDataRespostaDesc(Long idAluno);

    Page<RespostaAluno> findByAlunoIdAlunoOrderByDataRespostaDesc(Long idAluno, Pageable pageable);

    List<RespostaAluno> findByAlunoIdAlunoAndCasoClinicoIdCaso(Long idAluno, Long idCaso);

    List<RespostaAluno> findByCasoClinicoProfessorId(Long idProfessor);

    long countByAlunoIdAluno(Long idAluno);

    long countByAlunoIdAlunoAndCorretaTrue(Long idAluno);

    long countByCasoClinicoProfessorId(Long idProfessor);

    long countByCasoClinicoProfessorIdAndCorretaTrue(Long idProfessor);
}
