package com.SistemaApiCrud.SistemaCrud.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.SistemaApiCrud.SistemaCrud.dto.PerguntasGeradasIaDTO;
import com.SistemaApiCrud.SistemaCrud.dto.PerguntaRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.PerguntaResponseDTO;
import com.SistemaApiCrud.SistemaCrud.entity.enums.OperacaoGeracaoIa;
import com.SistemaApiCrud.SistemaCrud.repository.CasoClinicoRepository;

@Service
public class PerguntaIaTransactionService {

    private static final String VERSAO_PROMPT = "perguntas-v2";

    private final PerguntaService perguntaService;
    private final CasoClinicoRepository casoRepository;
    private final GeracaoIaAuditService auditService;
    private final IdempotenciaGeracaoIaStore idempotenciaStore;

    public PerguntaIaTransactionService(
            PerguntaService perguntaService,
            CasoClinicoRepository casoRepository,
            GeracaoIaAuditService auditService,
            IdempotenciaGeracaoIaStore idempotenciaStore) {
        this.perguntaService = perguntaService;
        this.casoRepository = casoRepository;
        this.auditService = auditService;
        this.idempotenciaStore = idempotenciaStore;
    }

    @Transactional
    public List<PerguntaResponseDTO> salvarComAuditoria(
            Long idCaso,
            List<PerguntaRequestDTO> perguntas,
            String fingerprint,
            long quantidadePerguntasExistentes,
            String contexto,
            PerguntasGeradasIaDTO saidaIa,
            RespostaIaComMetricas<PerguntasGeradasIaDTO> metricas) {
        List<PerguntaResponseDTO> respostas = perguntaService.salvarLoteEmCaso(
                idCaso,
                perguntas,
                fingerprint,
                quantidadePerguntasExistentes);
        String referencias = respostas.stream()
                .map(PerguntaResponseDTO::getId)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        auditService.registrar(
                casoRepository.getReferenceById(idCaso),
                OperacaoGeracaoIa.GERAR_PERGUNTAS,
                VERSAO_PROMPT,
                contexto,
                saidaIa,
                "perguntas:" + referencias,
                respostas.size(),
                metricas);
        Long idSolicitacao = ContextoIdempotenciaGeracaoIa.idAtual();
        if (idSolicitacao != null) {
            idempotenciaStore.concluirNaTransacaoAtual(
                    idSolicitacao,
                    201,
                    respostas.stream().map(PerguntaResponseDTO::getId).toList());
        }
        return respostas;
    }
}
