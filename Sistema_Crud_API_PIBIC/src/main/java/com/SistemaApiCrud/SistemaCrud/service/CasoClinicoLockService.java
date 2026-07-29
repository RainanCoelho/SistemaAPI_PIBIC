package com.SistemaApiCrud.SistemaCrud.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.SistemaApiCrud.SistemaCrud.entity.casos_clinicos;
import com.SistemaApiCrud.SistemaCrud.exception.RecursoNaoEncontradoException;
import com.SistemaApiCrud.SistemaCrud.repository.caso_clinico_repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@Service
public class CasoClinicoLockService {

    private final caso_clinico_repository casoRepository;
    private final EntityManager entityManager;
    private final AutorizacaoUsuarioService autorizacaoService;

    public CasoClinicoLockService(
            caso_clinico_repository casoRepository,
            EntityManager entityManager,
            AutorizacaoUsuarioService autorizacaoService) {
        this.casoRepository = casoRepository;
        this.entityManager = entityManager;
        this.autorizacaoService = autorizacaoService;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public casos_clinicos bloquearRascunho(Long idCaso) {
        return bloquearRascunhos(java.util.List.of(idCaso)).get(idCaso);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Map<Long, casos_clinicos> bloquearRascunhos(Collection<Long> idsCasos) {
        Map<Long, casos_clinicos> casos = new LinkedHashMap<>();
        idsCasos.stream()
                .distinct()
                .sorted()
                .forEach(idCaso -> {
                    casos_clinicos caso = casoRepository.findByIdForUpdate(idCaso)
                            .orElseThrow(() -> new RecursoNaoEncontradoException(
                                    "Caso clinico nao encontrado"));
                    entityManager.refresh(caso, LockModeType.PESSIMISTIC_WRITE);
                    autorizacaoService.validarAcessoCaso(caso);
                    CasoClinicoPolicy.validarRascunho(caso);
                    casos.put(idCaso, caso);
                });
        return casos;
    }
}
