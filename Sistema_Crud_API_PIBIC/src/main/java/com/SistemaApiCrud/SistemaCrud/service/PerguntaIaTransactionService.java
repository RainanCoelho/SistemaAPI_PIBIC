package com.SistemaApiCrud.SistemaCrud.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.SistemaApiCrud.SistemaCrud.DTO.PerguntasGeradasIaDTO;
import com.SistemaApiCrud.SistemaCrud.DTO.pergunta_request_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.pergunta_response_DTO;
import com.SistemaApiCrud.SistemaCrud.entity.enums.OperacaoGeracaoIa;
import com.SistemaApiCrud.SistemaCrud.repository.caso_clinico_repository;

@Service
public class PerguntaIaTransactionService {

    private static final String VERSAO_PROMPT = "perguntas-v2";

    private final pergunta_service perguntaService;
    private final caso_clinico_repository casoRepository;
    private final GeracaoIaAuditService auditService;

    public PerguntaIaTransactionService(
            pergunta_service perguntaService,
            caso_clinico_repository casoRepository,
            GeracaoIaAuditService auditService) {
        this.perguntaService = perguntaService;
        this.casoRepository = casoRepository;
        this.auditService = auditService;
    }

    @Transactional
    public List<pergunta_response_DTO> salvarComAuditoria(
            Long idCaso,
            List<pergunta_request_DTO> perguntas,
            String fingerprint,
            long quantidadePerguntasExistentes,
            String contexto,
            PerguntasGeradasIaDTO saidaIa) {
        List<pergunta_response_DTO> respostas = perguntaService.salvarLoteEmCaso(
                idCaso,
                perguntas,
                fingerprint,
                quantidadePerguntasExistentes);
        String referencias = respostas.stream()
                .map(pergunta_response_DTO::getId)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        auditService.registrar(
                casoRepository.getReferenceById(idCaso),
                OperacaoGeracaoIa.GERAR_PERGUNTAS,
                VERSAO_PROMPT,
                contexto,
                saidaIa,
                "perguntas:" + referencias,
                respostas.size());
        return respostas;
    }
}
