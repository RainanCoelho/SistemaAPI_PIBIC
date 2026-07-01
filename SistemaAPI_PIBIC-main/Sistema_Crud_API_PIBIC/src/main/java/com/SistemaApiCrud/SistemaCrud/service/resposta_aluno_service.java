package com.SistemaApiCrud.SistemaCrud.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.SistemaApiCrud.SistemaCrud.DTO.desempenho_aluno_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.historico_aluno_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.relatorio_desempenho_professor_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.responder_caso_request_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.resposta_aluno_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.resposta_pergunta_request_DTO;
import com.SistemaApiCrud.SistemaCrud.DTO.resultado_caso_DTO;
import com.SistemaApiCrud.SistemaCrud.entity.AlternativaPergunta;
import com.SistemaApiCrud.SistemaCrud.entity.Aluno;
import com.SistemaApiCrud.SistemaCrud.entity.RespostaAluno;
import com.SistemaApiCrud.SistemaCrud.entity.casos_clinicos;
import com.SistemaApiCrud.SistemaCrud.entity.enums.StatusCasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.pergunta;
import com.SistemaApiCrud.SistemaCrud.exception.BadRequestException;
import com.SistemaApiCrud.SistemaCrud.exception.BusinessException;
import com.SistemaApiCrud.SistemaCrud.exception.RecursoNaoEncontradoException;
import com.SistemaApiCrud.SistemaCrud.repository.alternativa_pergunta_repository;
import com.SistemaApiCrud.SistemaCrud.repository.aluno_repository;
import com.SistemaApiCrud.SistemaCrud.repository.caso_clinico_repository;
import com.SistemaApiCrud.SistemaCrud.repository.pergunta_repository;
import com.SistemaApiCrud.SistemaCrud.repository.professor_repository;
import com.SistemaApiCrud.SistemaCrud.repository.resposta_aluno_repository;

@Service
public class resposta_aluno_service {

    @Autowired
    private resposta_aluno_repository repository;

    @Autowired
    private aluno_repository alunoRepository;

    @Autowired
    private caso_clinico_repository casoRepository;

    @Autowired
    private pergunta_repository perguntaRepository;

    @Autowired
    private alternativa_pergunta_repository alternativaRepository;

    @Autowired
    private professor_repository professorRepository;

    @Autowired
    private TentativaCasoService tentativaCasoService;

    @Transactional
    public resultado_caso_DTO responderCaso(Long idAluno, Long idCaso, responder_caso_request_DTO request) {
        Aluno aluno = alunoRepository.findById(idAluno)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno nao encontrado"));

        casos_clinicos caso = casoRepository.findById(idCaso)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Caso clinico nao encontrado"));

        if (caso.getStatus() != StatusCasoClinico.PUBLICADO) {
            throw new BusinessException("O caso clinico ainda nao esta publicado");
        }

        if (!repository.findByAlunoIdAlunoAndCasoClinicoIdCaso(idAluno, idCaso).isEmpty()) {
            throw new BusinessException("O aluno ja respondeu este caso clinico");
        }

        var tentativa = tentativaCasoService.validarPrazo(idAluno, idCaso);
        List<pergunta> perguntasDoCaso = perguntaRepository.findByCasoClinicoIdCaso(idCaso);
        validarRespostasCompletas(perguntasDoCaso, request);

        List<RespostaAluno> respostas = request.getRespostas()
                .stream()
                .map(resposta -> criarResposta(aluno, caso, resposta))
                .toList();

        List<RespostaAluno> respostasSalvas = repository.saveAll(respostas);
        tentativaCasoService.finalizar(tentativa);
        return montarResultado(idAluno, idCaso, respostasSalvas);
    }

    private void validarRespostasCompletas(
            List<pergunta> perguntasDoCaso,
            responder_caso_request_DTO request) {
        if (perguntasDoCaso.isEmpty()) {
            throw new BusinessException("O caso clinico nao possui perguntas");
        }

        Set<Long> idsRecebidos = request.getRespostas()
                .stream()
                .map(resposta_pergunta_request_DTO::getIdPergunta)
                .collect(Collectors.toSet());

        if (idsRecebidos.size() != request.getRespostas().size()) {
            throw new BadRequestException("Cada pergunta deve ser respondida uma unica vez");
        }

        Set<Long> idsEsperados = perguntasDoCaso.stream()
                .map(pergunta::getId)
                .collect(Collectors.toSet());

        if (!idsRecebidos.equals(idsEsperados)) {
            throw new BadRequestException("Todas as perguntas do caso devem ser respondidas exatamente uma vez");
        }
    }

    public historico_aluno_DTO buscarHistorico(Long idAluno, Pageable pageable) {
        if (!alunoRepository.existsById(idAluno)) {
            throw new RecursoNaoEncontradoException("Aluno nao encontrado");
        }

        Page<resposta_aluno_DTO> respostas = repository
                .findByAlunoIdAlunoOrderByDataRespostaDesc(idAluno, pageable)
                .map(this::paraDTO);

        return new historico_aluno_DTO(idAluno, respostas);
    }

    public desempenho_aluno_DTO buscarDesempenho(Long idAluno) {
        if (!alunoRepository.existsById(idAluno)) {
            throw new RecursoNaoEncontradoException("Aluno nao encontrado");
        }

        long total = repository.countByAlunoIdAluno(idAluno);
        long corretas = repository.countByAlunoIdAlunoAndCorretaTrue(idAluno);

        return new desempenho_aluno_DTO(idAluno, total, corretas, calcularAproveitamento(total, corretas));
    }

    public relatorio_desempenho_professor_DTO gerarRelatorioProfessor(Long idProfessor) {
        if (!professorRepository.existsById(idProfessor)) {
            throw new RecursoNaoEncontradoException("Professor nao encontrado");
        }

        long total = repository.countByCasoClinicoProfessorId(idProfessor);
        long corretas = repository.countByCasoClinicoProfessorIdAndCorretaTrue(idProfessor);

        return new relatorio_desempenho_professor_DTO(idProfessor, total, corretas, calcularAproveitamento(total, corretas));
    }

    private RespostaAluno criarResposta(Aluno aluno, casos_clinicos caso, resposta_pergunta_request_DTO respostaRequest) {
        pergunta pergunta = perguntaRepository.findById(respostaRequest.getIdPergunta())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pergunta nao encontrada"));

        if (pergunta.getCasoClinico() == null || !pergunta.getCasoClinico().getIdCaso().equals(caso.getIdCaso())) {
            throw new BadRequestException("A pergunta informada nao pertence ao caso clinico");
        }

        RespostaAluno respostaAluno = new RespostaAluno();
        respostaAluno.setAluno(aluno);
        respostaAluno.setCasoClinico(caso);
        respostaAluno.setPergunta(pergunta);
        respostaAluno.setRespostaMarcada(respostaRequest.getRespostaMarcada());
        respostaAluno.setCorreta(compararResposta(pergunta, respostaRequest.getRespostaMarcada()));

        return respostaAluno;
    }

    private boolean compararResposta(pergunta pergunta, String respostaMarcada) {
        List<AlternativaPergunta> alternativas = alternativaRepository.findByPerguntaIdOrderByLetra(pergunta.getId());
        if (!alternativas.isEmpty()) {
            return alternativas.stream()
                    .anyMatch(alternativa -> Boolean.TRUE.equals(alternativa.getCorreta())
                            && correspondeResposta(alternativa, respostaMarcada));
        }

        String gabarito = pergunta.getGabarito();

        if (gabarito == null || gabarito.isBlank()) {
            gabarito = pergunta.getResposta();
        }

        return gabarito != null && respostaMarcada != null && gabarito.trim().equalsIgnoreCase(respostaMarcada.trim());
    }

    private boolean correspondeResposta(AlternativaPergunta alternativa, String respostaMarcada) {
        if (respostaMarcada == null) {
            return false;
        }

        String resposta = respostaMarcada.trim();
        return corresponde(resposta, alternativa.getLetra()) || corresponde(resposta, alternativa.getTexto());
    }

    private boolean corresponde(String valor, String referencia) {
        return valor != null && referencia != null && valor.equalsIgnoreCase(referencia.trim());
    }

    private resultado_caso_DTO montarResultado(Long idAluno, Long idCaso, List<RespostaAluno> respostas) {
        int total = respostas.size();
        int corretas = (int) respostas.stream().filter(resposta -> Boolean.TRUE.equals(resposta.getCorreta())).count();

        List<resposta_aluno_DTO> respostasDTO = respostas.stream()
                .map(this::paraDTO)
                .toList();

        return new resultado_caso_DTO(idAluno, idCaso, total, corretas, calcularAproveitamento(total, corretas), respostasDTO);
    }

    private Double calcularAproveitamento(long total, long corretas) {
        if (total == 0) {
            return 0.0;
        }

        return (corretas * 100.0) / total;
    }

    private resposta_aluno_DTO paraDTO(RespostaAluno resposta) {
        resposta_aluno_DTO dto = new resposta_aluno_DTO();
        dto.setId(resposta.getId());

        if (resposta.getAluno() != null) {
            dto.setIdAluno(resposta.getAluno().getIdAluno());
        }

        if (resposta.getCasoClinico() != null) {
            dto.setIdCaso(resposta.getCasoClinico().getIdCaso());
        }

        if (resposta.getPergunta() != null) {
            dto.setIdPergunta(resposta.getPergunta().getId());
        }

        dto.setRespostaMarcada(resposta.getRespostaMarcada());
        dto.setCorreta(resposta.getCorreta());
        dto.setDataResposta(resposta.getDataResposta());

        return dto;
    }
}
