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

import com.SistemaApiCrud.SistemaCrud.DTO.conteudo_clinico_DTO;
import com.SistemaApiCrud.SistemaCrud.service.AutorizacaoUsuarioService;
import com.SistemaApiCrud.SistemaCrud.service.conteudo_clinico_service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/conteudos")
public class conteudo_clinico_controller {

    private final conteudo_clinico_service service;
    private final AutorizacaoUsuarioService autorizacaoService;

    public conteudo_clinico_controller(
            conteudo_clinico_service service,
            AutorizacaoUsuarioService autorizacaoService) {
        this.service = service;
        this.autorizacaoService = autorizacaoService;
    }

    @GetMapping
    public Page<conteudo_clinico_DTO> listar(@PageableDefault(size = 20, sort = "idConteudo") Pageable pageable) {
        Long idProfessor = autorizacaoService.isAdmin()
                ? null
                : autorizacaoService.getIdProfessorAutenticado();
        return service.listar(pageable, idProfessor);
    }

    @GetMapping("/{id}")
    public ResponseEntity<conteudo_clinico_DTO> buscarPorId(@PathVariable @Min(1) Long id) {
        autorizacaoService.validarAcessoConteudo(id);
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<conteudo_clinico_DTO> salvar(@RequestBody @Valid conteudo_clinico_DTO conteudo) {
        autorizacaoService.validarAcessoCaso(conteudo.getIdCaso());
        conteudo_clinico_DTO conteudoSalvo = service.salvar(conteudo);
        return ResponseEntity.status(HttpStatus.CREATED).body(conteudoSalvo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<conteudo_clinico_DTO> atualizar(@PathVariable @Min(1) Long id,
                                                          @RequestBody @Valid conteudo_clinico_DTO conteudo) {
        autorizacaoService.validarAcessoConteudo(id);
        autorizacaoService.validarAcessoCaso(conteudo.getIdCaso());
        return ResponseEntity.ok(service.atualizar(id, conteudo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable @Min(1) Long id) {
        autorizacaoService.validarAcessoConteudo(id);
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
