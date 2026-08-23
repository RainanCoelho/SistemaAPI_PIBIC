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

import com.SistemaApiCrud.SistemaCrud.dto.CasoClinicoResponseDTO;
import com.SistemaApiCrud.SistemaCrud.dto.ProfessorCadastroRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.ProfessorRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.ProfessorResponseDTO;
import com.SistemaApiCrud.SistemaCrud.dto.RelatorioDesempenhoProfessorDTO;
import com.SistemaApiCrud.SistemaCrud.service.AutorizacaoUsuarioService;
import com.SistemaApiCrud.SistemaCrud.service.CasoClinicoService;
import com.SistemaApiCrud.SistemaCrud.service.ProfessorService;
import com.SistemaApiCrud.SistemaCrud.service.RespostaAlunoService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/professores")
public class ProfessorController {

    private final ProfessorService service;
    private final CasoClinicoService casoService;
    private final RespostaAlunoService respostaService;
    private final AutorizacaoUsuarioService autorizacaoService;

    public ProfessorController(
            ProfessorService service,
            CasoClinicoService casoService,
            RespostaAlunoService respostaService,
            AutorizacaoUsuarioService autorizacaoService) {
        this.service = service;
        this.casoService = casoService;
        this.respostaService = respostaService;
        this.autorizacaoService = autorizacaoService;
    }

    @GetMapping
    public Page<ProfessorResponseDTO> listar(@PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return service.listar(pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfessorResponseDTO> buscarPorId(@PathVariable @Min(1) Long id) {
        autorizacaoService.validarAcessoProfessor(id);
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping("/{id}/casos")
    public Page<CasoClinicoResponseDTO> listarCasos(
            @PathVariable @Min(1) Long id,
            @PageableDefault(size = 20, sort = "dataCriacao", direction = Sort.Direction.DESC) Pageable pageable) {
        autorizacaoService.validarAcessoProfessor(id);
        return casoService.listarPorProfessor(id, pageable);
    }

    @GetMapping("/{id}/relatorio-desempenho")
    public ResponseEntity<RelatorioDesempenhoProfessorDTO> gerarRelatorioDesempenho(@PathVariable @Min(1) Long id) {
        autorizacaoService.validarAcessoProfessor(id);
        return ResponseEntity.ok(respostaService.gerarRelatorioProfessor(id));
    }

    @PostMapping
    public ResponseEntity<ProfessorResponseDTO> salvar(@RequestBody @Valid ProfessorRequestDTO professor) {
        ProfessorResponseDTO professorSalvo = service.salvar(professor);
        return ResponseEntity.status(HttpStatus.CREATED).body(professorSalvo);
    }

    @PostMapping("/cadastro")
    public ResponseEntity<ProfessorResponseDTO> cadastrarPublico(
            @RequestBody @Valid ProfessorCadastroRequestDTO professor) {
        ProfessorResponseDTO professorSalvo = service.cadastrarPublico(professor);
        return ResponseEntity.status(HttpStatus.CREATED).body(professorSalvo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfessorResponseDTO> atualizar(@PathVariable @Min(1) Long id,
                                                            @RequestBody @Valid ProfessorRequestDTO professor) {
        autorizacaoService.validarAcessoProfessor(id);
        return ResponseEntity.ok(service.atualizar(id, professor));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable @Min(1) Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
