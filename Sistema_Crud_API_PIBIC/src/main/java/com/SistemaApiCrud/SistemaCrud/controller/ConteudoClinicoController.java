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

import com.SistemaApiCrud.SistemaCrud.dto.ConteudoClinicoDTO;
import com.SistemaApiCrud.SistemaCrud.service.AutorizacaoUsuarioService;
import com.SistemaApiCrud.SistemaCrud.service.ConteudoClinicoService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/conteudos")
public class ConteudoClinicoController {

    private final ConteudoClinicoService service;
    private final AutorizacaoUsuarioService autorizacaoService;

    public ConteudoClinicoController(
            ConteudoClinicoService service,
            AutorizacaoUsuarioService autorizacaoService) {
        this.service = service;
        this.autorizacaoService = autorizacaoService;
    }

    @GetMapping
    public Page<ConteudoClinicoDTO> listar(@PageableDefault(size = 20, sort = "idConteudo") Pageable pageable) {
        Long idProfessor = autorizacaoService.isAdmin()
                ? null
                : autorizacaoService.getIdProfessorAutenticado();
        return service.listar(pageable, idProfessor);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConteudoClinicoDTO> buscarPorId(@PathVariable @Min(1) Long id) {
        autorizacaoService.validarAcessoConteudo(id);
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<ConteudoClinicoDTO> salvar(@RequestBody @Valid ConteudoClinicoDTO conteudo) {
        autorizacaoService.validarAcessoCaso(conteudo.getIdCaso());
        ConteudoClinicoDTO conteudoSalvo = service.salvar(conteudo);
        return ResponseEntity.status(HttpStatus.CREATED).body(conteudoSalvo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConteudoClinicoDTO> atualizar(@PathVariable @Min(1) Long id,
                                                          @RequestBody @Valid ConteudoClinicoDTO conteudo) {
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
