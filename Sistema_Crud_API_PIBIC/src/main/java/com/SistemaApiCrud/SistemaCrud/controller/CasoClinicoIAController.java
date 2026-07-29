package com.SistemaApiCrud.SistemaCrud.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.SistemaApiCrud.SistemaCrud.DTO.CasoClinicoAjusteRequestDTO;
import com.SistemaApiCrud.SistemaCrud.DTO.CasoClinicoRequestDTO;
import com.SistemaApiCrud.SistemaCrud.DTO.CasoClinicoResponseDTO;
import com.SistemaApiCrud.SistemaCrud.DTO.GerarPerguntasIaRequestDTO;
import com.SistemaApiCrud.SistemaCrud.DTO.pergunta_response_DTO;
import com.SistemaApiCrud.SistemaCrud.service.AutorizacaoUsuarioService;
import com.SistemaApiCrud.SistemaCrud.service.PerguntaIaService;
import com.SistemaApiCrud.SistemaCrud.service.ServicoCasoClinicoIa;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/casos/{id}/ia")
public class CasoClinicoIAController {

    private final ServicoCasoClinicoIa servicoCasoClinicoIa;
    private final PerguntaIaService servicoPerguntaIa;
    private final AutorizacaoUsuarioService servicoAutorizacao;

    public CasoClinicoIAController(
            ServicoCasoClinicoIa servicoCasoClinicoIa,
            PerguntaIaService servicoPerguntaIa,
            AutorizacaoUsuarioService servicoAutorizacao) {
        this.servicoCasoClinicoIa = servicoCasoClinicoIa;
        this.servicoPerguntaIa = servicoPerguntaIa;
        this.servicoAutorizacao = servicoAutorizacao;
    }

    @PostMapping("/gerar")
    public ResponseEntity<CasoClinicoResponseDTO> gerarConteudo(
            @PathVariable @Min(1) Long id,
            @RequestBody @Valid CasoClinicoRequestDTO requisicao) {
        servicoAutorizacao.validarAcessoCaso(id);
        return ResponseEntity.ok(servicoCasoClinicoIa.gerarConteudo(id, requisicao));
    }

    @PostMapping("/ajustar")
    public ResponseEntity<CasoClinicoResponseDTO> ajustarConteudo(
            @PathVariable @Min(1) Long id,
            @RequestBody @Valid CasoClinicoAjusteRequestDTO requisicao) {
        servicoAutorizacao.validarAcessoCaso(id);
        return ResponseEntity.ok(servicoCasoClinicoIa.ajustarConteudo(id, requisicao));
    }

    @PostMapping("/perguntas/gerar")
    public ResponseEntity<List<pergunta_response_DTO>> gerarPerguntas(
            @PathVariable @Min(1) Long id,
            @RequestBody @Valid GerarPerguntasIaRequestDTO requisicao) {
        servicoAutorizacao.validarAcessoCaso(id);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicoPerguntaIa.gerarPerguntas(id, requisicao));
    }
}
