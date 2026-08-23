package com.SistemaApiCrud.SistemaCrud.controller;

import java.util.List;

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
import org.springframework.web.bind.annotation.RestController;

import com.SistemaApiCrud.SistemaCrud.dto.CasoClinicoAjusteRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.CasoClinicoIaRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.CasoClinicoIaResponseDTO;
import com.SistemaApiCrud.SistemaCrud.dto.AuditoriaGeracaoIaDTO;
import com.SistemaApiCrud.SistemaCrud.dto.GerarPerguntasIaRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.PerguntaResponseDTO;
import com.SistemaApiCrud.SistemaCrud.service.AutorizacaoUsuarioService;
import com.SistemaApiCrud.SistemaCrud.service.PerguntaIaService;
import com.SistemaApiCrud.SistemaCrud.service.ServicoCasoClinicoIa;
import com.SistemaApiCrud.SistemaCrud.service.GeracaoIaAuditService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/casos/{id}/ia")
public class CasoClinicoIAController {

    private final ServicoCasoClinicoIa servicoCasoClinicoIa;
    private final PerguntaIaService servicoPerguntaIa;
    private final AutorizacaoUsuarioService servicoAutorizacao;
    private final GeracaoIaAuditService auditService;

    public CasoClinicoIAController(
            ServicoCasoClinicoIa servicoCasoClinicoIa,
            PerguntaIaService servicoPerguntaIa,
            AutorizacaoUsuarioService servicoAutorizacao,
            GeracaoIaAuditService auditService) {
        this.servicoCasoClinicoIa = servicoCasoClinicoIa;
        this.servicoPerguntaIa = servicoPerguntaIa;
        this.servicoAutorizacao = servicoAutorizacao;
        this.auditService = auditService;
    }

    @PostMapping("/gerar")
    public ResponseEntity<CasoClinicoIaResponseDTO> gerarConteudo(
            @PathVariable @Min(1) Long id,
            @RequestBody @Valid CasoClinicoIaRequestDTO requisicao) {
        servicoAutorizacao.validarAcessoCaso(id);
        return ResponseEntity.ok(servicoCasoClinicoIa.gerarConteudo(id, requisicao));
    }

    @PostMapping("/ajustar")
    public ResponseEntity<CasoClinicoIaResponseDTO> ajustarConteudo(
            @PathVariable @Min(1) Long id,
            @RequestBody @Valid CasoClinicoAjusteRequestDTO requisicao) {
        servicoAutorizacao.validarAcessoCaso(id);
        return ResponseEntity.ok(servicoCasoClinicoIa.ajustarConteudo(id, requisicao));
    }

    @PostMapping("/perguntas/gerar")
    public ResponseEntity<List<PerguntaResponseDTO>> gerarPerguntas(
            @PathVariable @Min(1) Long id,
            @RequestBody @Valid GerarPerguntasIaRequestDTO requisicao) {
        servicoAutorizacao.validarAcessoCaso(id);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicoPerguntaIa.gerarPerguntas(id, requisicao));
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
}
