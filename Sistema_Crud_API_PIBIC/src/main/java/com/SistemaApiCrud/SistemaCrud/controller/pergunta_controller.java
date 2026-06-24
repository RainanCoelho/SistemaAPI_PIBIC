package com.SistemaApiCrud.SistemaCrud.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.SistemaApiCrud.SistemaCrud.DTO.pergunta_request_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.pergunta_response_DTO;
import com.SistemaApiCrud.SistemaCrud.service.AutorizacaoUsuarioService;
import com.SistemaApiCrud.SistemaCrud.service.pergunta_service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/perguntas")
public class pergunta_controller {

    private final pergunta_service service;
    private final AutorizacaoUsuarioService autorizacaoService;

    public pergunta_controller(pergunta_service service, AutorizacaoUsuarioService autorizacaoService) {
        this.service = service;
        this.autorizacaoService = autorizacaoService;
    }

    @GetMapping
    public Page<pergunta_response_DTO> listar(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        if (autorizacaoService.isAdmin()) {
            return service.listar(pageable);
        }

        return service.listarPorProfessor(autorizacaoService.getIdProfessorAutenticado(), pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<pergunta_response_DTO> buscarPorId(@PathVariable @Min(1) Long id) {
        autorizacaoService.validarAcessoPergunta(id);
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping("/caso/{casoId}")
    public Page<pergunta_response_DTO> listarPorCaso(
            @PathVariable @Min(1) Long casoId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        autorizacaoService.validarAcessoCaso(casoId);
        return service.listarPorCaso(casoId, pageable);
    }

    @PostMapping
    public ResponseEntity<pergunta_response_DTO> salvar(@RequestBody @Valid pergunta_request_DTO pergunta) {
        if (pergunta.getIdCaso() != null) {
            autorizacaoService.validarAcessoCaso(pergunta.getIdCaso());
        }

        pergunta_response_DTO perguntaSalva = service.salvar(pergunta);
        return ResponseEntity.status(HttpStatus.CREATED).body(perguntaSalva);
    }

    @PostMapping("/caso/{casoId}")
    public ResponseEntity<pergunta_response_DTO> salvarEmCaso(@PathVariable @Min(1) Long casoId,
                                                              @RequestBody @Valid pergunta_request_DTO pergunta) {
        autorizacaoService.validarAcessoCaso(casoId);
        pergunta_response_DTO perguntaSalva = service.salvarEmCaso(casoId, pergunta);
        return ResponseEntity.status(HttpStatus.CREATED).body(perguntaSalva);
    }

    @PutMapping("/{id}")
    public ResponseEntity<pergunta_response_DTO> atualizar(@PathVariable @Min(1) Long id,
                                                           @RequestBody @Valid pergunta_request_DTO pergunta) {
        autorizacaoService.validarAcessoPergunta(id);
        if (pergunta.getIdCaso() != null) {
            autorizacaoService.validarAcessoCaso(pergunta.getIdCaso());
        }

        return ResponseEntity.ok(service.atualizar(id, pergunta));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable @Min(1) Long id) {
        autorizacaoService.validarAcessoPergunta(id);
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
