package com.SistemaApiCrud.SistemaCrud.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import com.SistemaApiCrud.SistemaCrud.dto.ConteudoClinicoDTO;
import com.SistemaApiCrud.SistemaCrud.entity.CasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.ConteudoClinico;
import com.SistemaApiCrud.SistemaCrud.exception.BadRequestException;
import com.SistemaApiCrud.SistemaCrud.exception.RecursoNaoEncontradoException;
import com.SistemaApiCrud.SistemaCrud.repository.ConteudoClinicoRepository;

@Service
public class ConteudoClinicoService {

    private final ConteudoClinicoRepository repository;
    private final CasoClinicoLockService casoLockService;

    public ConteudoClinicoService(
            ConteudoClinicoRepository repository,
            CasoClinicoLockService casoLockService) {
        this.repository = repository;
        this.casoLockService = casoLockService;
    }

    public Page<ConteudoClinicoDTO> listar(Pageable pageable, Long idProfessor) {
        if (idProfessor == null) {
            return repository.findAll(pageable).map(this::paraDTO);
        }
        return repository.findByCasoClinicoProfessorId(idProfessor, pageable).map(this::paraDTO);
    }

    public ConteudoClinicoDTO buscarPorId(Long id) {
        return paraDTO(buscarEntityPorId(id));
    }

    @Transactional
    public ConteudoClinicoDTO salvar(ConteudoClinicoDTO dto) {
        ConteudoClinico conteudo = new ConteudoClinico();
        CasoClinico caso = casoLockService.bloquearRascunho(idCasoObrigatorio(dto));
        aplicarDados(dto, conteudo, caso);
        ConteudoClinico conteudoSalvo = repository.save(conteudo);
        return paraDTO(conteudoSalvo);
    }

    @Transactional
    public ConteudoClinicoDTO atualizar(Long id, ConteudoClinicoDTO dto) {
        ConteudoClinico conteudo = buscarEntityPorIdParaAtualizacao(id);
        Long idCasoAtual = conteudo.getCasoClinico().getIdCaso();
        Long idCasoDestino = idCasoObrigatorio(dto);
        Map<Long, CasoClinico> casos = casoLockService.bloquearRascunhos(
                List.of(idCasoAtual, idCasoDestino));
        aplicarDados(dto, conteudo, casos.get(idCasoDestino));
        return paraDTO(conteudo);
    }

    @Transactional
    public void deletar(Long id) {
        ConteudoClinico conteudo = buscarEntityPorIdParaAtualizacao(id);
        casoLockService.bloquearRascunho(conteudo.getCasoClinico().getIdCaso());
        repository.deleteById(id);
    }

    private ConteudoClinicoDTO paraDTO(ConteudoClinico conteudo) {
        ConteudoClinicoDTO dto = new ConteudoClinicoDTO();

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
            ConteudoClinicoDTO dto,
            ConteudoClinico conteudo,
            CasoClinico caso) {
        conteudo.setCasoClinico(caso);
        conteudo.setSintomas(dto.getSintomas());
        conteudo.setContexto(dto.getContexto());
        conteudo.setExamClinico(dto.getExamClinico());
        conteudo.setAntecClinico(dto.getAntecClinico());
        conteudo.setDiagEsperado(dto.getDiagEsperado());
    }

    private Long idCasoObrigatorio(ConteudoClinicoDTO dto) {
        if (dto.getIdCaso() == null) {
            throw new BadRequestException("O caso clinico e obrigatorio");
        }
        return dto.getIdCaso();
    }

    private ConteudoClinico buscarEntityPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conteudo clinico nao encontrado"));
    }

    private ConteudoClinico buscarEntityPorIdParaAtualizacao(Long id) {
        return repository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conteudo clinico nao encontrado"));
    }
}
