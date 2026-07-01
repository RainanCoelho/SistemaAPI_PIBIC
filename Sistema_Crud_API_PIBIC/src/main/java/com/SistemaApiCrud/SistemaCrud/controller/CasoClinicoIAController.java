package com.SistemaApiCrud.SistemaCrud.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.SistemaApiCrud.SistemaCrud.DTO.CasoClinicoRequestDTO;
import com.SistemaApiCrud.SistemaCrud.DTO.CasoClinicoResponseDTO;
import com.SistemaApiCrud.SistemaCrud.service.AutorizacaoUsuarioService;
import com.SistemaApiCrud.SistemaCrud.service.GroqService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/casos/{id}/ia")
public class CasoClinicoIAController {

    private final GroqService groqService;
    private final AutorizacaoUsuarioService autorizacaoService;

    public CasoClinicoIAController(GroqService groqService, AutorizacaoUsuarioService autorizacaoService) {
        this.groqService = groqService;
        this.autorizacaoService = autorizacaoService;
    }

    @PostMapping("/gerar")
    public ResponseEntity<CasoClinicoResponseDTO> gerarConteudo(
            @PathVariable @Min(1) Long id,
            @RequestBody @Valid CasoClinicoRequestDTO dto) {
        autorizacaoService.validarAcessoCaso(id);
        return ResponseEntity.ok(groqService.gerarConteudo(id, dto));
    }
}
