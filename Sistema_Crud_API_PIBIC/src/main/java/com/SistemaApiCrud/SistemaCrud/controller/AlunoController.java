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

import com.SistemaApiCrud.SistemaCrud.dto.AlunoRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.AlunoResponseDTO;
import com.SistemaApiCrud.SistemaCrud.dto.CasoClinicoAlunoDTO;
import com.SistemaApiCrud.SistemaCrud.dto.CasoClinicoResponseDTO;
import com.SistemaApiCrud.SistemaCrud.dto.DesempenhoAlunoDTO;
import com.SistemaApiCrud.SistemaCrud.dto.HistoricoAlunoDTO;
import com.SistemaApiCrud.SistemaCrud.dto.ResponderCasoRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.ResultadoCasoDTO;
import com.SistemaApiCrud.SistemaCrud.service.AutorizacaoUsuarioService;
import com.SistemaApiCrud.SistemaCrud.service.AlunoService;
import com.SistemaApiCrud.SistemaCrud.service.CasoClinicoService;
import com.SistemaApiCrud.SistemaCrud.service.RespostaAlunoService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/alunos")
public class AlunoController {

    private final AlunoService service;
    private final CasoClinicoService casoService;
    private final RespostaAlunoService respostaService;
    private final AutorizacaoUsuarioService autorizacaoService;

    public AlunoController(
            AlunoService service,
            CasoClinicoService casoService,
            RespostaAlunoService respostaService,
            AutorizacaoUsuarioService autorizacaoService) {
        this.service = service;
        this.casoService = casoService;
        this.respostaService = respostaService;
        this.autorizacaoService = autorizacaoService;
    }

    @GetMapping
    public Page<AlunoResponseDTO> listar(@PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return service.listar(pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlunoResponseDTO> buscarPorId(@PathVariable @Min(1) Long id) {
        autorizacaoService.validarAcessoAluno(id);
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping("/{id}/casos-disponiveis")
    public Page<CasoClinicoResponseDTO> listarCasosDisponiveis(
            @PathVariable @Min(1) Long id,
            @PageableDefault(size = 20, sort = "dataCriacao", direction = Sort.Direction.DESC) Pageable pageable) {
        autorizacaoService.validarAcessoAluno(id);
        service.buscarPorId(id);
        return casoService.listarPublicados(pageable);
    }

    @GetMapping("/{id}/casos/{casoId}/completo")
    public ResponseEntity<CasoClinicoAlunoDTO> buscarCasoDisponivelCompleto(
            @PathVariable @Min(1) Long id,
            @PathVariable @Min(1) Long casoId) {
        autorizacaoService.validarAcessoAluno(id);
        service.buscarPorId(id);
        return ResponseEntity.ok(casoService.buscarCompletoPublicadoPorId(casoId, id));
    }

    @GetMapping("/{id}/historico")
    public ResponseEntity<HistoricoAlunoDTO> buscarHistorico(
            @PathVariable @Min(1) Long id,
            @PageableDefault(size = 20, sort = "dataResposta", direction = Sort.Direction.DESC) Pageable pageable) {
        autorizacaoService.validarAcessoAluno(id);
        return ResponseEntity.ok(respostaService.buscarHistorico(id, pageable));
    }

    @GetMapping("/{id}/desempenho")
    public ResponseEntity<DesempenhoAlunoDTO> buscarDesempenho(@PathVariable @Min(1) Long id) {
        autorizacaoService.validarAcessoAluno(id);
        return ResponseEntity.ok(respostaService.buscarDesempenho(id));
    }

    @PostMapping
    public ResponseEntity<AlunoResponseDTO> salvar(@RequestBody @Valid AlunoRequestDTO aluno) {
        AlunoResponseDTO alunoSalvo = service.salvar(aluno);
        return ResponseEntity.status(HttpStatus.CREATED).body(alunoSalvo);
    }

    @PostMapping("/{id}/casos/{casoId}/responder")
    public ResponseEntity<ResultadoCasoDTO> responderCaso(@PathVariable @Min(1) Long id,
                                                            @PathVariable @Min(1) Long casoId,
                                                            @RequestBody @Valid ResponderCasoRequestDTO request) {
        autorizacaoService.validarAcessoAluno(id);
        ResultadoCasoDTO resultado = respostaService.responderCaso(id, casoId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlunoResponseDTO> atualizar(@PathVariable @Min(1) Long id,
                                                        @RequestBody @Valid AlunoRequestDTO aluno) {
        autorizacaoService.validarAcessoAluno(id);
        return ResponseEntity.ok(service.atualizar(id, aluno));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable @Min(1) Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
