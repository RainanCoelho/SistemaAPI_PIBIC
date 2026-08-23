package com.SistemaApiCrud.SistemaCrud.service;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.SistemaApiCrud.SistemaCrud.entity.CasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.ConteudoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.Paciente;
import com.SistemaApiCrud.SistemaCrud.exception.ConflitoEstadoException;
import com.SistemaApiCrud.SistemaCrud.repository.ConteudoClinicoRepository;
import com.SistemaApiCrud.SistemaCrud.repository.PacienteRepository;

@Service
public class CasoClinicoIaTransactionService {

    private final CasoClinicoLockService casoLockService;
    private final ConteudoClinicoRepository conteudoRepository;
    private final PacienteRepository pacienteRepository;
    private final IdempotenciaGeracaoIaStore idempotenciaStore;

    public CasoClinicoIaTransactionService(
            CasoClinicoLockService casoLockService,
            ConteudoClinicoRepository conteudoRepository,
            PacienteRepository pacienteRepository,
            IdempotenciaGeracaoIaStore idempotenciaStore) {
        this.casoLockService = casoLockService;
        this.conteudoRepository = conteudoRepository;
        this.pacienteRepository = pacienteRepository;
        this.idempotenciaStore = idempotenciaStore;
    }

    @Transactional
    public <T> T executarGeracao(
            Long idCaso,
            String fingerprintEsperado,
            List<Long> idsPacientesEsperados,
            Function<CasoClinico, T> operacao) {
        bloquearPacientesEsperados(idCaso, idsPacientesEsperados);
        CasoClinico caso = casoLockService.bloquearRascunho(idCaso);
        validarContextoInalterado(caso, fingerprintEsperado);
        return concluirIdempotencia(operacao.apply(caso));
    }

    @Transactional
    public <T> T executarAjuste(
            Long idCaso,
            Long idConteudoEsperado,
            String fingerprintEsperado,
            List<Long> idsPacientesEsperados,
            BiFunction<CasoClinico, ConteudoClinico, T> operacao) {
        bloquearPacientesEsperados(idCaso, idsPacientesEsperados);
        ConteudoClinico conteudo = conteudoRepository.findByIdForUpdate(idConteudoEsperado)
                .orElseThrow(() -> contextoAlterado(
                        "O conteudo clinico mudou durante o ajuste; tente novamente"));
        CasoClinico caso = casoLockService.bloquearRascunho(idCaso);

        if (conteudo.getCasoClinico() == null
                || !idCaso.equals(conteudo.getCasoClinico().getIdCaso())) {
            throw contextoAlterado(
                    "O conteudo clinico mudou durante o ajuste; tente novamente");
        }

        validarContextoInalterado(caso, fingerprintEsperado);
        ConteudoClinico conteudoMaisRecente = conteudoRepository
                .findFirstByCasoClinicoIdCasoOrderByIdConteudoDesc(idCaso)
                .orElseThrow(() -> contextoAlterado(
                        "O conteudo clinico mudou durante o ajuste; tente novamente"));
        if (!idConteudoEsperado.equals(conteudoMaisRecente.getIdConteudo())) {
            throw contextoAlterado(
                    "Um novo conteudo clinico foi criado durante o ajuste; tente novamente");
        }

        return concluirIdempotencia(operacao.apply(caso, conteudo));
    }

    private void bloquearPacientesEsperados(
            Long idCaso,
            List<Long> idsPacientesEsperados) {
        List<Long> idsOrdenados = idsPacientesEsperados == null
                ? List.of()
                : idsPacientesEsperados.stream().sorted().toList();

        for (Long idPaciente : idsOrdenados) {
            Paciente paciente = pacienteRepository.findByIdForUpdate(idPaciente)
                    .orElseThrow(() -> contextoAlterado(
                            "Um paciente mudou durante a operacao com IA; tente novamente"));
            if (paciente.getCasoClinico() == null
                    || !idCaso.equals(paciente.getCasoClinico().getIdCaso())) {
                throw contextoAlterado(
                        "Um paciente mudou de caso durante a operacao com IA; tente novamente");
            }
        }
    }

    private void validarContextoInalterado(
            CasoClinico caso,
            String fingerprintEsperado) {
        ConteudoClinico conteudoAtual = conteudoRepository
                .findFirstByCasoClinicoIdCasoOrderByIdConteudoDesc(caso.getIdCaso())
                .orElse(null);
        List<Paciente> pacientesAtuais = pacienteRepository
                .findByCasoClinicoIdCasoOrderByIdPacienteAsc(caso.getIdCaso());
        String fingerprintAtual = CasoClinicoFingerprint.calcular(
                caso,
                conteudoAtual,
                pacientesAtuais);

        if (!fingerprintEsperado.equals(fingerprintAtual)) {
            throw contextoAlterado(
                    "O caso clinico mudou durante a operacao com IA; tente novamente");
        }
    }

    private ConflitoEstadoException contextoAlterado(String mensagem) {
        return new ConflitoEstadoException(mensagem);
    }

    private <T> T concluirIdempotencia(T resposta) {
        Long idSolicitacao = ContextoIdempotenciaGeracaoIa.idAtual();
        if (idSolicitacao != null && resposta instanceof com.SistemaApiCrud.SistemaCrud.dto.CasoClinicoIaResponseDTO dto) {
            idempotenciaStore.concluirNaTransacaoAtual(idSolicitacao, 200, List.of(dto.getIdConteudo()));
        }
        return resposta;
    }
}
