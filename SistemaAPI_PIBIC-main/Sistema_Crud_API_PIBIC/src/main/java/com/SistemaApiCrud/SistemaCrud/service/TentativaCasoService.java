package com.SistemaApiCrud.SistemaCrud.service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.SistemaApiCrud.SistemaCrud.entity.Aluno;
import com.SistemaApiCrud.SistemaCrud.entity.TentativaCaso;
import com.SistemaApiCrud.SistemaCrud.entity.casos_clinicos;
import com.SistemaApiCrud.SistemaCrud.exception.BusinessException;
import com.SistemaApiCrud.SistemaCrud.exception.RecursoNaoEncontradoException;
import com.SistemaApiCrud.SistemaCrud.repository.aluno_repository;
import com.SistemaApiCrud.SistemaCrud.repository.tentativa_caso_repository;

@Service
public class TentativaCasoService {

    private final tentativa_caso_repository repository;
    private final aluno_repository alunoRepository;

    public TentativaCasoService(
            tentativa_caso_repository repository,
            aluno_repository alunoRepository) {
        this.repository = repository;
        this.alunoRepository = alunoRepository;
    }

    @Transactional
    public TentativaCaso iniciarOuBuscar(Long idAluno, casos_clinicos caso) {
        TentativaCaso tentativa = repository
                .findByAlunoIdAlunoAndCasoClinicoIdCaso(idAluno, caso.getIdCaso())
                .orElseGet(() -> criarTentativa(idAluno, caso));

        validarEmAndamento(tentativa, Instant.now());
        return tentativa;
    }

    public TentativaCaso validarPrazo(Long idAluno, Long idCaso) {
        TentativaCaso tentativa = repository
                .findByAlunoIdAlunoAndCasoClinicoIdCaso(idAluno, idCaso)
                .orElseThrow(() -> new BusinessException(
                        "A tentativa deve ser iniciada antes do envio das respostas"));

        validarEmAndamento(tentativa, Instant.now());
        return tentativa;
    }

    @Transactional
    public void finalizar(TentativaCaso tentativa) {
        tentativa.setDataFinalizacao(agora());
        repository.save(tentativa);
    }

    public long calcularSegundosRestantes(TentativaCaso tentativa) {
        return Math.max(0, Duration.between(Instant.now(), tentativa.getDataLimite()).toSeconds());
    }

    private TentativaCaso criarTentativa(Long idAluno, casos_clinicos caso) {
        Aluno aluno = alunoRepository.findById(idAluno)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno nao encontrado"));
        Instant inicio = agora();

        TentativaCaso tentativa = new TentativaCaso();
        tentativa.setAluno(aluno);
        tentativa.setCasoClinico(caso);
        tentativa.setDataInicio(inicio);
        tentativa.setDataLimite(inicio.plus(caso.getTempoLimiteMinutos(), ChronoUnit.MINUTES));
        return repository.save(tentativa);
    }

    private void validarEmAndamento(TentativaCaso tentativa, Instant agora) {
        if (tentativa.getDataFinalizacao() != null) {
            throw new BusinessException("O aluno ja respondeu este caso clinico");
        }
        if (!tentativa.getDataLimite().isAfter(agora)) {
            throw new BusinessException("O tempo limite para responder o caso clinico expirou");
        }
    }

    private Instant agora() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
