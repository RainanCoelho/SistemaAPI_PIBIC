package com.SistemaApiCrud.SistemaCrud.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.SistemaApiCrud.SistemaCrud.entity.Usuario;

public interface usuario_repository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    Optional<Usuario> findByAlunoIdAluno(Long idAluno);

    Optional<Usuario> findByProfessorId(Long idProfessor);

    boolean existsByUsername(String username);
}
