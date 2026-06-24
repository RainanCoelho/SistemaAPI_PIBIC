package com.SistemaApiCrud.SistemaCrud.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.SistemaApiCrud.SistemaCrud.DTO.usuario_request_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.usuario_response_DTO;
import com.SistemaApiCrud.SistemaCrud.service.usuario_service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/usuarios")
public class usuario_controller {

    private final usuario_service service;

    public usuario_controller(usuario_service service) {
        this.service = service;
    }

    @GetMapping
    public Page<usuario_response_DTO> listar(@PageableDefault(size = 20, sort = "username") Pageable pageable) {
        return service.listar(pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<usuario_response_DTO> buscarPorId(@PathVariable @Min(1) Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<usuario_response_DTO> salvar(@RequestBody @Valid usuario_request_DTO usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.salvar(usuario));
    }

    @PutMapping("/{id}")
    public ResponseEntity<usuario_response_DTO> atualizar(
            @PathVariable @Min(1) Long id,
            @RequestBody @Valid usuario_request_DTO usuario) {
        return ResponseEntity.ok(service.atualizar(id, usuario));
    }

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<usuario_response_DTO> ativar(@PathVariable @Min(1) Long id) {
        return ResponseEntity.ok(service.ativar(id));
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<usuario_response_DTO> desativar(@PathVariable @Min(1) Long id) {
        return ResponseEntity.ok(service.desativar(id));
    }
}
