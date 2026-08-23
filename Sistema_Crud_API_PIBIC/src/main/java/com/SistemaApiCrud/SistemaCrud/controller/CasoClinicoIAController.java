package com.SistemaApiCrud.SistemaCrud.controller;

import java.util.List;
import java.util.Arrays;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.SistemaApiCrud.SistemaCrud.dto.CasoClinicoAjusteRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.CasoClinicoIaRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.CasoClinicoIaResponseDTO;
import com.SistemaApiCrud.SistemaCrud.dto.AuditoriaGeracaoIaDTO;
import com.SistemaApiCrud.SistemaCrud.dto.GerarPerguntasIaRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.PerguntaResponseDTO;
import com.SistemaApiCrud.SistemaCrud.dto.ConteudoClinicoDTO;
import com.SistemaApiCrud.SistemaCrud.service.AutorizacaoUsuarioService;
import com.SistemaApiCrud.SistemaCrud.service.CasoClinicoService;
import com.SistemaApiCrud.SistemaCrud.service.PerguntaIaService;
import com.SistemaApiCrud.SistemaCrud.service.ServicoCasoClinicoIa;
import com.SistemaApiCrud.SistemaCrud.service.GeracaoIaAuditService;
import com.SistemaApiCrud.SistemaCrud.service.IdempotenciaGeracaoIaService;
import com.SistemaApiCrud.SistemaCrud.service.RespostaIdempotente;
import com.SistemaApiCrud.SistemaCrud.service.PerguntaService;
import com.SistemaApiCrud.SistemaCrud.entity.SolicitacaoGeracaoIa;
import com.SistemaApiCrud.SistemaCrud.entity.enums.OperacaoGeracaoIa;
import com.SistemaApiCrud.SistemaCrud.exception.ConflitoIdempotenciaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/casos/{id}/ia")
public class CasoClinicoIAController {

    private static final Logger LOG = LoggerFactory.getLogger(CasoClinicoIAController.class);

    private final ServicoCasoClinicoIa servicoCasoClinicoIa;
    private final PerguntaIaService servicoPerguntaIa;
    private final AutorizacaoUsuarioService servicoAutorizacao;
    private final GeracaoIaAuditService auditService;
    private final CasoClinicoService casoClinicoService;
    private final IdempotenciaGeracaoIaService idempotenciaService;
    private final PerguntaService perguntaService;

    public CasoClinicoIAController(
            ServicoCasoClinicoIa servicoCasoClinicoIa,
            PerguntaIaService servicoPerguntaIa,
            AutorizacaoUsuarioService servicoAutorizacao,
            GeracaoIaAuditService auditService,
            CasoClinicoService casoClinicoService,
            IdempotenciaGeracaoIaService idempotenciaService,
            PerguntaService perguntaService) {
        this.servicoCasoClinicoIa = servicoCasoClinicoIa;
        this.servicoPerguntaIa = servicoPerguntaIa;
        this.servicoAutorizacao = servicoAutorizacao;
        this.auditService = auditService;
        this.casoClinicoService = casoClinicoService;
        this.idempotenciaService = idempotenciaService;
        this.perguntaService = perguntaService;
    }

    @PostMapping("/gerar")
    public ResponseEntity<CasoClinicoIaResponseDTO> gerarConteudo(
            @PathVariable @Min(1) Long id,
            @RequestBody @Valid CasoClinicoIaRequestDTO requisicao,
            @RequestHeader(value = "Idempotency-Key", required = false) String chaveIdempotencia) {
        servicoAutorizacao.validarAcessoCaso(id);
        RespostaIdempotente<CasoClinicoIaResponseDTO> resposta = idempotenciaService.executar(
                chaveIdempotencia,
                OperacaoGeracaoIa.GERAR_CASO,
                id,
                requisicao,
                HttpStatus.OK.value(),
                this::reconstruirRespostaCaso,
                () -> servicoCasoClinicoIa.gerarConteudo(id, requisicao));
        return ResponseEntity.status(resposta.status()).body(enriquecerRespostaCaso(resposta.corpo(), id));
    }

    @PostMapping("/ajustar")
    public ResponseEntity<CasoClinicoIaResponseDTO> ajustarConteudo(
            @PathVariable @Min(1) Long id,
            @RequestBody @Valid CasoClinicoAjusteRequestDTO requisicao,
            @RequestHeader(value = "Idempotency-Key", required = false) String chaveIdempotencia) {
        servicoAutorizacao.validarAcessoCaso(id);
        RespostaIdempotente<CasoClinicoIaResponseDTO> resposta = idempotenciaService.executar(
                chaveIdempotencia,
                OperacaoGeracaoIa.AJUSTAR_CASO,
                id,
                requisicao,
                HttpStatus.OK.value(),
                this::reconstruirRespostaCaso,
                () -> servicoCasoClinicoIa.ajustarConteudo(id, requisicao));
        return ResponseEntity.status(resposta.status()).body(enriquecerRespostaCaso(resposta.corpo(), id));
    }

    @PostMapping("/perguntas/gerar")
    public ResponseEntity<List<PerguntaResponseDTO>> gerarPerguntas(
            @PathVariable @Min(1) Long id,
            @RequestBody @Valid GerarPerguntasIaRequestDTO requisicao,
            @RequestHeader(value = "Idempotency-Key", required = false) String chaveIdempotencia) {
        servicoAutorizacao.validarAcessoCaso(id);
        RespostaIdempotente<List<PerguntaResponseDTO>> resposta = idempotenciaService.executar(
                chaveIdempotencia,
                OperacaoGeracaoIa.GERAR_PERGUNTAS,
                id,
                requisicao,
                HttpStatus.CREATED.value(),
                this::reconstruirPerguntas,
                () -> servicoPerguntaIa.gerarPerguntas(id, requisicao));
        return ResponseEntity.status(resposta.status()).body(resposta.corpo());
    }

    @GetMapping("/auditoria")
    public Page<AuditoriaGeracaoIaDTO> listarAuditoria(
            @PathVariable @Min(1) Long id,
            @PageableDefault(
                    size = 20,
                    sort = "dataGeracao",
                    direction = Sort.Direction.DESC)
            Pageable pageable) {
        servicoAutorizacao.validarAcessoCaso(id);
        return auditService.listarPorCaso(id, pageable);
    }

    private CasoClinicoIaResponseDTO enriquecerRespostaCaso(CasoClinicoIaResponseDTO resposta, Long idCaso) {
        try {
            resposta.setCompleto(casoClinicoService.buscarCompletoPorId(idCaso));
        } catch (RuntimeException ex) {
            LOG.warn("Nao foi possivel enriquecer a resposta de IA do caso {} apos a persistencia", idCaso);
        }
        return resposta;
    }

    private CasoClinicoIaResponseDTO reconstruirRespostaCaso(SolicitacaoGeracaoIa solicitacao) {
        if (solicitacao.getIdCaso() == null) {
            throw new ConflitoIdempotenciaException();
        }
        Long idConteudo = idsResultado(solicitacao).stream().findFirst()
                .orElseThrow(ConflitoIdempotenciaException::new);
        ConteudoClinicoDTO conteudo = casoClinicoService.buscarCompletoPorId(solicitacao.getIdCaso())
                .getConteudosClinicos().stream()
                .filter(item -> idConteudo.equals(item.getIdConteudo()))
                .findFirst()
                .orElseThrow(ConflitoIdempotenciaException::new);
        CasoClinicoIaResponseDTO resposta = new CasoClinicoIaResponseDTO();
        resposta.setIdCaso(solicitacao.getIdCaso());
        resposta.setIdConteudo(conteudo.getIdConteudo());
        resposta.setSintomas(conteudo.getSintomas());
        resposta.setContexto(conteudo.getContexto());
        resposta.setExamClinico(conteudo.getExamClinico());
        resposta.setAntecClinico(conteudo.getAntecClinico());
        resposta.setDiagEsperado(conteudo.getDiagEsperado());
        return resposta;
    }

    private List<PerguntaResponseDTO> reconstruirPerguntas(SolicitacaoGeracaoIa solicitacao) {
        return idsResultado(solicitacao).stream().map(perguntaService::buscarPorId).toList();
    }

    private List<Long> idsResultado(SolicitacaoGeracaoIa solicitacao) {
        if (solicitacao.getIdsResultado() == null || solicitacao.getIdsResultado().isBlank()) {
            throw new ConflitoIdempotenciaException();
        }
        try {
            return Arrays.stream(solicitacao.getIdsResultado().split(","))
                    .map(Long::valueOf)
                    .toList();
        } catch (NumberFormatException ex) {
            throw new ConflitoIdempotenciaException();
        }
    }
}
