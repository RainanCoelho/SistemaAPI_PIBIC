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

import com.SistemaApiCrud.SistemaCrud.dto.UsuarioRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.UsuarioResponseDTO;
import com.SistemaApiCrud.SistemaCrud.service.UsuarioService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public Page<UsuarioResponseDTO> listar(@PageableDefault(size = 20, sort = "username") Pageable pageable) {
        return service.listar(pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> buscarPorId(@PathVariable @Min(1) Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<UsuarioResponseDTO> salvar(@RequestBody @Valid UsuarioRequestDTO usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.salvar(usuario));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponseDTO> atualizar(
            @PathVariable @Min(1) Long id,
            @RequestBody @Valid UsuarioRequestDTO usuario) {
        return ResponseEntity.ok(service.atualizar(id, usuario));
    }

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<UsuarioResponseDTO> ativar(@PathVariable @Min(1) Long id) {
        return ResponseEntity.ok(service.ativar(id));
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<UsuarioResponseDTO> desativar(@PathVariable @Min(1) Long id) {
        return ResponseEntity.ok(service.desativar(id));
    }
}
