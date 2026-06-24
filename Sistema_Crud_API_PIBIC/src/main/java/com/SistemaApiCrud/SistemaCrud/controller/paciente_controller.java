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

import com.SistemaApiCrud.SistemaCrud.DTO.paciente_DTO;
import com.SistemaApiCrud.SistemaCrud.service.AutorizacaoUsuarioService;
import com.SistemaApiCrud.SistemaCrud.service.paciente_service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/pacientes")
public class paciente_controller {

    private final paciente_service service;
    private final AutorizacaoUsuarioService autorizacaoService;

    public paciente_controller(
            paciente_service service,
            AutorizacaoUsuarioService autorizacaoService) {
        this.service = service;
        this.autorizacaoService = autorizacaoService;
    }

    @GetMapping
    public Page<paciente_DTO> listar(@PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        Long idProfessor = autorizacaoService.isAdmin()
                ? null
                : autorizacaoService.getIdProfessorAutenticado();
        return service.listar(pageable, idProfessor);
    }

    @GetMapping("/{id}")
    public ResponseEntity<paciente_DTO> buscarPorId(@PathVariable @Min(1) Long id) {
        autorizacaoService.validarAcessoPaciente(id);
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<paciente_DTO> salvar(@RequestBody @Valid paciente_DTO paciente) {
        autorizacaoService.validarAcessoCaso(paciente.getIdCaso());
        paciente_DTO pacienteSalvo = service.salvar(paciente);
        return ResponseEntity.status(HttpStatus.CREATED).body(pacienteSalvo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<paciente_DTO> atualizar(@PathVariable @Min(1) Long id,
                                                  @RequestBody @Valid paciente_DTO paciente) {
        autorizacaoService.validarAcessoPaciente(id);
        autorizacaoService.validarAcessoCaso(paciente.getIdCaso());
        return ResponseEntity.ok(service.atualizar(id, paciente));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable @Min(1) Long id) {
        autorizacaoService.validarAcessoPaciente(id);
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
