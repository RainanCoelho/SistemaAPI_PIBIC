package com.SistemaApiCrud.SistemaCrud.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import com.SistemaApiCrud.SistemaCrud.DTO.conteudo_clinico_DTO;
import com.SistemaApiCrud.SistemaCrud.entity.casos_clinicos;
import com.SistemaApiCrud.SistemaCrud.entity.conteudo_clinico;
import com.SistemaApiCrud.SistemaCrud.exception.BadRequestException;
import com.SistemaApiCrud.SistemaCrud.exception.RecursoNaoEncontradoException;
import com.SistemaApiCrud.SistemaCrud.repository.conteudo_clinico_repository;

@Service
public class conteudo_clinico_service {

    private final conteudo_clinico_repository repository;
    private final CasoClinicoLockService casoLockService;

    public conteudo_clinico_service(
            conteudo_clinico_repository repository,
            CasoClinicoLockService casoLockService) {
        this.repository = repository;
        this.casoLockService = casoLockService;
    }

    public Page<conteudo_clinico_DTO> listar(Pageable pageable, Long idProfessor) {
        if (idProfessor == null) {
            return repository.findAll(pageable).map(this::paraDTO);
        }
        return repository.findByCasoClinicoProfessorId(idProfessor, pageable).map(this::paraDTO);
    }

    public conteudo_clinico_DTO buscarPorId(Long id) {
        return paraDTO(buscarEntityPorId(id));
    }

    @Transactional
    public conteudo_clinico_DTO salvar(conteudo_clinico_DTO dto) {
        conteudo_clinico conteudo = new conteudo_clinico();
        casos_clinicos caso = casoLockService.bloquearRascunho(idCasoObrigatorio(dto));
        aplicarDados(dto, conteudo, caso);
        conteudo_clinico conteudoSalvo = repository.save(conteudo);
        return paraDTO(conteudoSalvo);
    }

    @Transactional
    public conteudo_clinico_DTO atualizar(Long id, conteudo_clinico_DTO dto) {
        conteudo_clinico conteudo = buscarEntityPorIdParaAtualizacao(id);
        Long idCasoAtual = conteudo.getCasoClinico().getIdCaso();
        Long idCasoDestino = idCasoObrigatorio(dto);
        Map<Long, casos_clinicos> casos = casoLockService.bloquearRascunhos(
                List.of(idCasoAtual, idCasoDestino));
        aplicarDados(dto, conteudo, casos.get(idCasoDestino));
        return paraDTO(conteudo);
    }

    @Transactional
    public void deletar(Long id) {
        conteudo_clinico conteudo = buscarEntityPorIdParaAtualizacao(id);
        casoLockService.bloquearRascunho(conteudo.getCasoClinico().getIdCaso());
        repository.deleteById(id);
    }

    private conteudo_clinico_DTO paraDTO(conteudo_clinico conteudo) {
        conteudo_clinico_DTO dto = new conteudo_clinico_DTO();

        dto.setIdConteudo(conteudo.getIdConteudo());

        if (conteudo.getCasoClinico() != null) {
            dto.setIdCaso(conteudo.getCasoClinico().getIdCaso());
        }

        dto.setSintomas(conteudo.getSintomas());
        dto.setContexto(conteudo.getContexto());
        dto.setExamClinico(conteudo.getExamClinico());
        dto.setAntecClinico(conteudo.getAntecClinico());
        dto.setDiagEsperado(conteudo.getDiagEsperado());

        return dto;
    }

    private void aplicarDados(
            conteudo_clinico_DTO dto,
            conteudo_clinico conteudo,
            casos_clinicos caso) {
        conteudo.setCasoClinico(caso);
        conteudo.setSintomas(dto.getSintomas());
        conteudo.setContexto(dto.getContexto());
        conteudo.setExamClinico(dto.getExamClinico());
        conteudo.setAntecClinico(dto.getAntecClinico());
        conteudo.setDiagEsperado(dto.getDiagEsperado());
    }

    private Long idCasoObrigatorio(conteudo_clinico_DTO dto) {
        if (dto.getIdCaso() == null) {
            throw new BadRequestException("O caso clinico e obrigatorio");
        }
        return dto.getIdCaso();
    }

    private conteudo_clinico buscarEntityPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conteudo clinico nao encontrado"));
    }

    private conteudo_clinico buscarEntityPorIdParaAtualizacao(Long id) {
        return repository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conteudo clinico nao encontrado"));
    }
}
