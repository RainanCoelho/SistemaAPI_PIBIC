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

import com.SistemaApiCrud.SistemaCrud.dto.CasoClinicoCompletoDTO;
import com.SistemaApiCrud.SistemaCrud.dto.CasoClinicoRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.CasoClinicoResponseDTO;
import com.SistemaApiCrud.SistemaCrud.dto.PerguntaRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.PerguntaResponseDTO;
import com.SistemaApiCrud.SistemaCrud.dto.RevisarRespostaRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.RevisaoRespostaDTO;
import com.SistemaApiCrud.SistemaCrud.dto.RespostaAlunoDTO;
import com.SistemaApiCrud.SistemaCrud.entity.enums.StatusCasoClinico;
import com.SistemaApiCrud.SistemaCrud.service.AutorizacaoUsuarioService;
import com.SistemaApiCrud.SistemaCrud.service.CasoClinicoService;
import com.SistemaApiCrud.SistemaCrud.service.PerguntaService;
import com.SistemaApiCrud.SistemaCrud.service.RespostaAlunoService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@Validated
@RestController
@RequestMapping("/casos")
public class CasoClinicoController {

    private final CasoClinicoService service;
    private final PerguntaService perguntaService;
    private final RespostaAlunoService servicoRespostaAluno;
    private final AutorizacaoUsuarioService autorizacaoService;

    public CasoClinicoController(
            CasoClinicoService service,
            PerguntaService perguntaService,
            RespostaAlunoService servicoRespostaAluno,
            AutorizacaoUsuarioService autorizacaoService) {
        this.service = service;
        this.perguntaService = perguntaService;
        this.servicoRespostaAluno = servicoRespostaAluno;
        this.autorizacaoService = autorizacaoService;
    }

    @GetMapping
    public Page<CasoClinicoResponseDTO> listar(
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
    public ResponseEntity<CasoClinicoResponseDTO> buscarPorId(@PathVariable @Min(1) Long id) {
        autorizacaoService.validarAcessoCaso(id);
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping("/{id}/completo")
    public ResponseEntity<CasoClinicoCompletoDTO> buscarCompletoPorId(@PathVariable @Min(1) Long id) {
        autorizacaoService.validarAcessoCaso(id);
        return ResponseEntity.ok(service.buscarCompletoPorId(id));
    }

    @GetMapping("/{casoId}/perguntas")
    public Page<PerguntaResponseDTO> listarPerguntas(
            @PathVariable @Min(1) Long casoId,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        autorizacaoService.validarAcessoCaso(casoId);
        return perguntaService.listarPorCaso(casoId, pageable);
    }

    @PostMapping
    public ResponseEntity<CasoClinicoResponseDTO> salvar(@RequestBody @Valid CasoClinicoRequestDTO caso) {
        Long idProfessor = autorizacaoService.resolverProfessorParaEscrita(caso.getIdProfessor());
        CasoClinicoResponseDTO casoSalvo = service.salvar(caso, idProfessor);
        return ResponseEntity.status(HttpStatus.CREATED).body(casoSalvo);
    }

    @PostMapping("/{casoId}/perguntas")
    public ResponseEntity<PerguntaResponseDTO> salvarPergunta(@PathVariable @Min(1) Long casoId,
                                                                @RequestBody @Valid PerguntaRequestDTO pergunta) {
        autorizacaoService.validarAcessoCaso(casoId);
        PerguntaResponseDTO perguntaSalva = perguntaService.salvarEmCaso(casoId, pergunta);
        return ResponseEntity.status(HttpStatus.CREATED).body(perguntaSalva);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CasoClinicoResponseDTO> atualizar(@PathVariable @Min(1) Long id,
                                                               @RequestBody @Valid CasoClinicoRequestDTO caso) {
        autorizacaoService.validarAcessoCaso(id);
        Long idProfessor = autorizacaoService.resolverProfessorParaEscrita(caso.getIdProfessor());
        return ResponseEntity.ok(service.atualizar(id, caso, idProfessor));
    }

    @PatchMapping("/{id}/publicar")
    public ResponseEntity<CasoClinicoResponseDTO> publicar(@PathVariable @Min(1) Long id) {
        autorizacaoService.validarAcessoCaso(id);
        return ResponseEntity.ok(service.publicar(id));
    }

    @PatchMapping("/{id}/arquivar")
    public ResponseEntity<CasoClinicoResponseDTO> arquivar(@PathVariable @Min(1) Long id) {
        autorizacaoService.validarAcessoCaso(id);
        return ResponseEntity.ok(service.arquivar(id));
    }

    @GetMapping("/{id}/respostas/pendentes-revisao")
    public Page<RespostaAlunoDTO> listarRespostasPendentesRevisao(
            @PathVariable @Min(1) Long id,
            @PageableDefault(size = 20, sort = "dataResposta") Pageable paginacao) {
        autorizacaoService.validarAcessoCaso(id);
        return servicoRespostaAluno.listarPendentesRevisao(id, paginacao);
    }

    @PatchMapping("/{id}/respostas/{idResposta}/revisao")
    public ResponseEntity<RevisaoRespostaDTO> revisarResposta(
            @PathVariable @Min(1) Long id,
            @PathVariable @Min(1) Long idResposta,
            @RequestBody @Valid RevisarRespostaRequestDTO requisicao) {
        autorizacaoService.validarAcessoCaso(id);
        return ResponseEntity.ok(servicoRespostaAluno.revisarResposta(
                id,
                idResposta,
                requisicao.getCorreta(),
                requisicao.getJustificativa(),
                autorizacaoService.getIdUsuarioAutenticado()));
    }

    @GetMapping("/{id}/respostas/{idResposta}/revisoes")
    public List<RevisaoRespostaDTO> listarHistoricoRevisoes(
            @PathVariable @Min(1) Long id,
            @PathVariable @Min(1) Long idResposta) {
        autorizacaoService.validarAcessoCaso(id);
        return servicoRespostaAluno.listarHistoricoRevisoes(id, idResposta);
    }

    @GetMapping("/{id}/respostas/{idResposta}/revisoes/pagina")
    public Page<RevisaoRespostaDTO> listarHistoricoRevisoesPaginado(
            @PathVariable @Min(1) Long id,
            @PathVariable @Min(1) Long idResposta,
            @PageableDefault(size = 20, sort = "versaoRevisao") Pageable paginacao) {
        autorizacaoService.validarAcessoCaso(id);
        return servicoRespostaAluno.listarHistoricoRevisoesPaginado(
                id,
                idResposta,
                paginacao);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable @Min(1) Long id) {
        autorizacaoService.validarAcessoCaso(id);
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
