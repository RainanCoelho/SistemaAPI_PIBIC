package com.SistemaApiCrud.SistemaCrud.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.SistemaApiCrud.SistemaCrud.DTO.caso_clinico_completo_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.caso_clinico_request_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.caso_clinico_response_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.pergunta_request_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.pergunta_response_DTO;
import com.SistemaApiCrud.SistemaCrud.entity.enums.StatusCasoClinico;
import com.SistemaApiCrud.SistemaCrud.service.AutorizacaoUsuarioService;
import com.SistemaApiCrud.SistemaCrud.service.caso_clinico_service;
import com.SistemaApiCrud.SistemaCrud.service.pergunta_service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@Validated
@RestController
@RequestMapping("/casos")
public class caso_clinico_controller {

    private final caso_clinico_service service;
    private final pergunta_service perguntaService;
    private final AutorizacaoUsuarioService autorizacaoService;

    public caso_clinico_controller(
            caso_clinico_service service,
            pergunta_service perguntaService,
            AutorizacaoUsuarioService autorizacaoService) {
        this.service = service;
        this.perguntaService = perguntaService;
        this.autorizacaoService = autorizacaoService;
    }

    @GetMapping
    public Page<caso_clinico_response_DTO> listar(
            @RequestParam(required = false) StatusCasoClinico status,
            @RequestParam(required = false) @Min(1) Long idProfessor,
            @RequestParam(required = false)
            @Size(max = 100, message = "O termo deve ter no maximo 100 caracteres")
            String termo,
            @PageableDefault(size = 20, sort = "dataCriacao", direction = Sort.Direction.DESC) Pageable pageable) {
        Long filtroProfessor = autorizacaoService.resolverFiltroProfessor(idProfessor);
        return service.listarPaginado(status, filtroProfessor, termo, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<caso_clinico_response_DTO> buscarPorId(@PathVariable @Min(1) Long id) {
        autorizacaoService.validarAcessoCaso(id);
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping("/{id}/completo")
    public ResponseEntity<caso_clinico_completo_DTO> buscarCompletoPorId(@PathVariable @Min(1) Long id) {
        autorizacaoService.validarAcessoCaso(id);
        return ResponseEntity.ok(service.buscarCompletoPorId(id));
    }

    @GetMapping("/{casoId}/perguntas")
    public Page<pergunta_response_DTO> listarPerguntas(
            @PathVariable @Min(1) Long casoId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        autorizacaoService.validarAcessoCaso(casoId);
        return perguntaService.listarPorCaso(casoId, pageable);
    }

    @PostMapping
    public ResponseEntity<caso_clinico_response_DTO> salvar(@RequestBody @Valid caso_clinico_request_DTO caso) {
        Long idProfessor = autorizacaoService.resolverProfessorParaEscrita(caso.getIdProfessor());
        caso_clinico_response_DTO casoSalvo = service.salvar(caso, idProfessor);
        return ResponseEntity.status(HttpStatus.CREATED).body(casoSalvo);
    }

    @PostMapping("/{casoId}/perguntas")
    public ResponseEntity<pergunta_response_DTO> salvarPergunta(@PathVariable @Min(1) Long casoId,
                                                                @RequestBody @Valid pergunta_request_DTO pergunta) {
        autorizacaoService.validarAcessoCaso(casoId);
        pergunta_response_DTO perguntaSalva = perguntaService.salvarEmCaso(casoId, pergunta);
        return ResponseEntity.status(HttpStatus.CREATED).body(perguntaSalva);
    }

    @PutMapping("/{id}")
    public ResponseEntity<caso_clinico_response_DTO> atualizar(@PathVariable @Min(1) Long id,
                                                               @RequestBody @Valid caso_clinico_request_DTO caso) {
        autorizacaoService.validarAcessoCaso(id);
        Long idProfessor = autorizacaoService.resolverProfessorParaEscrita(caso.getIdProfessor());
        return ResponseEntity.ok(service.atualizar(id, caso, idProfessor));
    }

    @PatchMapping("/{id}/publicar")
    public ResponseEntity<caso_clinico_response_DTO> publicar(@PathVariable @Min(1) Long id) {
        autorizacaoService.validarAcessoCaso(id);
        return ResponseEntity.ok(service.publicar(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable @Min(1) Long id) {
        autorizacaoService.validarAcessoCaso(id);
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
