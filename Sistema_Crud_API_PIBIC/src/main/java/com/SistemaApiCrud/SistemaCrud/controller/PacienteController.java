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

import com.SistemaApiCrud.SistemaCrud.dto.PacienteDTO;
import com.SistemaApiCrud.SistemaCrud.service.AutorizacaoUsuarioService;
import com.SistemaApiCrud.SistemaCrud.service.PacienteService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/pacientes")
public class PacienteController {

    private final PacienteService service;
    private final AutorizacaoUsuarioService autorizacaoService;

    public PacienteController(
            PacienteService service,
            AutorizacaoUsuarioService autorizacaoService) {
        this.service = service;
        this.autorizacaoService = autorizacaoService;
    }

    @GetMapping
    public Page<PacienteDTO> listar(@PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        Long idProfessor = autorizacaoService.isAdmin()
                ? null
                : autorizacaoService.getIdProfessorAutenticado();
        return service.listar(pageable, idProfessor);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PacienteDTO> buscarPorId(@PathVariable @Min(1) Long id) {
        autorizacaoService.validarAcessoPaciente(id);
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<PacienteDTO> salvar(@RequestBody @Valid PacienteDTO paciente) {
        autorizacaoService.validarAcessoCaso(paciente.getIdCaso());
        PacienteDTO pacienteSalvo = service.salvar(paciente);
        return ResponseEntity.status(HttpStatus.CREATED).body(pacienteSalvo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PacienteDTO> atualizar(@PathVariable @Min(1) Long id,
                                                  @RequestBody @Valid PacienteDTO paciente) {
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
