package com.SistemaApiCrud.SistemaCrud.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

import com.SistemaApiCrud.SistemaCrud.DTO.caso_clinico_response_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.professor_cadastro_request_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.professor_request_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.professor_response_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.relatorio_desempenho_professor_DTO;
import com.SistemaApiCrud.SistemaCrud.service.AutorizacaoUsuarioService;
import com.SistemaApiCrud.SistemaCrud.service.caso_clinico_service;
import com.SistemaApiCrud.SistemaCrud.service.professor_service;
import com.SistemaApiCrud.SistemaCrud.service.resposta_aluno_service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/professores")
public class professor_controller {

    private final professor_service service;
    private final caso_clinico_service casoService;
    private final resposta_aluno_service respostaService;
    private final AutorizacaoUsuarioService autorizacaoService;

    public professor_controller(
            professor_service service,
            caso_clinico_service casoService,
            resposta_aluno_service respostaService,
            AutorizacaoUsuarioService autorizacaoService) {
        this.service = service;
        this.casoService = casoService;
        this.respostaService = respostaService;
        this.autorizacaoService = autorizacaoService;
    }

    @GetMapping
    public Page<professor_response_DTO> listar(@PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return service.listar(pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<professor_response_DTO> buscarPorId(@PathVariable @Min(1) Long id) {
        autorizacaoService.validarAcessoProfessor(id);
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping("/{id}/casos")
    public Page<caso_clinico_response_DTO> listarCasos(
            @PathVariable @Min(1) Long id,
            @PageableDefault(size = 20, sort = "dataCriacao", direction = Sort.Direction.DESC) Pageable pageable) {
        autorizacaoService.validarAcessoProfessor(id);
        return casoService.listarPorProfessor(id, pageable);
    }

    @GetMapping("/{id}/relatorio-desempenho")
    public ResponseEntity<relatorio_desempenho_professor_DTO> gerarRelatorioDesempenho(@PathVariable @Min(1) Long id) {
        autorizacaoService.validarAcessoProfessor(id);
        return ResponseEntity.ok(respostaService.gerarRelatorioProfessor(id));
    }

    @PostMapping
    public ResponseEntity<professor_response_DTO> salvar(@RequestBody @Valid professor_request_DTO professor) {
        professor_response_DTO professorSalvo = service.salvar(professor);
        return ResponseEntity.status(HttpStatus.CREATED).body(professorSalvo);
    }

    @PostMapping("/cadastro")
    public ResponseEntity<professor_response_DTO> cadastrarPublico(
            @RequestBody @Valid professor_cadastro_request_DTO professor) {
        professor_response_DTO professorSalvo = service.cadastrarPublico(professor);
        return ResponseEntity.status(HttpStatus.CREATED).body(professorSalvo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<professor_response_DTO> atualizar(@PathVariable @Min(1) Long id,
                                                            @RequestBody @Valid professor_request_DTO professor) {
        autorizacaoService.validarAcessoProfessor(id);
        return ResponseEntity.ok(service.atualizar(id, professor));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable @Min(1) Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
