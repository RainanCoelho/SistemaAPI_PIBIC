package com.SistemaApiCrud.SistemaCrud.controller;


import com.SistemaApiCrud.SistemaCrud.DTO.CasoClinicoRequestDTO;
import com.SistemaApiCrud.SistemaCrud.DTO.CasoClinicoResponseDTO;
import com.SistemaApiCrud.SistemaCrud.service.AutorizacaoUsuarioService;
import com.SistemaApiCrud.SistemaCrud.service.GroqService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.io.IOException;

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
            @RequestBody @Valid CasoClinicoRequestDTO dto) throws IOException {

        autorizacaoService.validarAcessoCaso(id);
        CasoClinicoResponseDTO response = groqService.gerarConteudo(id, dto);
        return ResponseEntity.ok(response);
    }




}
