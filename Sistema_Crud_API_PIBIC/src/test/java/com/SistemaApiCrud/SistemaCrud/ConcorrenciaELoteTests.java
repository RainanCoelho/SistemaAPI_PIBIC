package com.SistemaApiCrud.SistemaCrud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.SistemaApiCrud.SistemaCrud.dto.PacienteDTO;
import com.SistemaApiCrud.SistemaCrud.dto.PerguntaRequestDTO;
import com.SistemaApiCrud.SistemaCrud.dto.SalvarPerguntasLoteDTO;
import com.SistemaApiCrud.SistemaCrud.entity.Aluno;
import com.SistemaApiCrud.SistemaCrud.entity.CasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.Paciente;
import com.SistemaApiCrud.SistemaCrud.entity.Professor;
import com.SistemaApiCrud.SistemaCrud.entity.enums.EstadoCivil;
import com.SistemaApiCrud.SistemaCrud.entity.enums.NivelDificuldade;
import com.SistemaApiCrud.SistemaCrud.entity.enums.Sexo;
import com.SistemaApiCrud.SistemaCrud.entity.enums.StatusCasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.enums.TipoPergunta;
import com.SistemaApiCrud.SistemaCrud.repository.AlunoRepository;
import com.SistemaApiCrud.SistemaCrud.repository.CasoClinicoRepository;
import com.SistemaApiCrud.SistemaCrud.repository.PacienteRepository;
import com.SistemaApiCrud.SistemaCrud.repository.PerguntaRepository;
import com.SistemaApiCrud.SistemaCrud.repository.ProfessorRepository;
import com.SistemaApiCrud.SistemaCrud.repository.TentativaCasoRepository;
import com.SistemaApiCrud.SistemaCrud.service.CasoClinicoService;
import com.SistemaApiCrud.SistemaCrud.service.PacienteService;
import com.SistemaApiCrud.SistemaCrud.service.PerguntaService;

@SpringBootTest
class ConcorrenciaELoteTests {

    @Autowired private CasoClinicoService casos;
    @Autowired private CasoClinicoRepository casoRepository;
    @Autowired private ProfessorRepository professorRepository;
    @Autowired private AlunoRepository alunoRepository;
    @Autowired private TentativaCasoRepository tentativaRepository;
    @Autowired private PerguntaService perguntas;
    @Autowired private PerguntaRepository perguntaRepository;
    @Autowired private PacienteRepository pacienteRepository;
    @Autowired private PacienteService pacientes;
    @Autowired private org.springframework.transaction.PlatformTransactionManager transactionManager;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    private com.SistemaApiCrud.SistemaCrud.service.CasoClinicoLockService locks;

    @BeforeEach
    void autenticar() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void primeirasAberturasSimultaneasCompartilhamTentativa() throws Exception {
        CasoClinico caso = caso(StatusCasoClinico.PUBLICADO);
        Aluno aluno = new Aluno();
        aluno.setNome("Aluno simulado");
        aluno.setEmail(UUID.randomUUID() + "@example.com");
        aluno.setCurso("Medicina");
        aluno.setPeriodo("1");
        Long idAluno = alunoRepository.saveAndFlush(aluno).getIdAluno();
        CountDownLatch inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var primeira = executor.submit(() -> {
                inicio.await();
                return casos.buscarCompletoPublicadoPorId(caso.getIdCaso(), idAluno);
            });
            var segunda = executor.submit(() -> {
                inicio.await();
                return casos.buscarCompletoPublicadoPorId(caso.getIdCaso(), idAluno);
            });
            inicio.countDown();
            assertThat(primeira.get(10, TimeUnit.SECONDS)).isNotNull();
            assertThat(segunda.get(10, TimeUnit.SECONDS)).isNotNull();
        }
        assertThat(tentativaRepository.findByAlunoIdAlunoAndCasoClinicoIdCaso(idAluno, caso.getIdCaso())).isPresent();
        casos.deletar(caso.getIdCaso());
    }

    @Test
    void loteInvalidoReverteEdicaoAnteriorENaoAceitaPerguntaDeOutroCaso() {
        CasoClinico caso = caso(StatusCasoClinico.RASCUNHO);
        var primeira = perguntas.salvarEmCaso(caso.getIdCaso(), pergunta("Original"));
        var invalida = pergunta("Invalida");
        invalida.setGabarito("nao-e-booleano");
        var lote = new SalvarPerguntasLoteDTO(List.of(
                new SalvarPerguntasLoteDTO.Item(primeira.getId(), pergunta("Alterada")),
                new SalvarPerguntasLoteDTO.Item(null, invalida)));
        assertThatThrownBy(() -> perguntas.salvarEdicoesEmLote(caso.getIdCaso(), lote)).isInstanceOf(RuntimeException.class);
        assertThat(perguntaRepository.findById(primeira.getId()).orElseThrow().getTexto()).isEqualTo("Original");
        assertThat(perguntaRepository.countByCasoClinicoIdCaso(caso.getIdCaso())).isOne();
        CasoClinico outro = caso(StatusCasoClinico.RASCUNHO);
        assertThatThrownBy(() -> perguntas.salvarEdicoesEmLote(outro.getIdCaso(), new SalvarPerguntasLoteDTO(
                List.of(new SalvarPerguntasLoteDTO.Item(primeira.getId(), pergunta("Outra"))))))
                .isInstanceOf(com.SistemaApiCrud.SistemaCrud.exception.BadRequestException.class);
        var salvo = perguntas.salvarEdicoesEmLote(caso.getIdCaso(), new SalvarPerguntasLoteDTO(List.of(
                new SalvarPerguntasLoteDTO.Item(primeira.getId(), pergunta("Alterada")),
                new SalvarPerguntasLoteDTO.Item(null, pergunta("Nova")))));
        assertThat(salvo).hasSize(2);
        assertThat(salvo.getFirst().getTexto()).isEqualTo("Alterada");
        assertThat(perguntaRepository.countByCasoClinicoIdCaso(caso.getIdCaso())).isEqualTo(2);
        casos.deletar(caso.getIdCaso());
        casos.deletar(outro.getIdCaso());
    }

    @Test
    void moverPacienteAtualizaVinculoAntesDaExclusaoDoCasoAntigo() {
        CasoClinico origem = caso(StatusCasoClinico.RASCUNHO);
        CasoClinico destino = caso(StatusCasoClinico.RASCUNHO);
        Paciente paciente = pacienteRepository.saveAndFlush(new Paciente(null, origem, "Simulado", "Docente",
                Sexo.NAO_INFORMADO, 30, EstadoCivil.NAO_INFORMADO, "170", "70"));
        PacienteDTO dto = pacientes.buscarPorId(paciente.getIdPaciente());
        dto.setIdCaso(destino.getIdCaso());
        pacientes.atualizar(paciente.getIdPaciente(), dto);
        casos.deletar(origem.getIdCaso());
        assertThat(pacientes.buscarPorId(paciente.getIdPaciente()).getIdCaso()).isEqualTo(destino.getIdCaso());
        casos.deletar(destino.getIdCaso());
    }

    @Test
    void edicaoEsperaPeloCasoSemReterLockDoPaciente() throws Exception {
        CasoClinico caso = caso(StatusCasoClinico.RASCUNHO);
        Paciente paciente = pacienteRepository.saveAndFlush(new Paciente(null, caso, "Simulado", "Docente",
                Sexo.NAO_INFORMADO, 30, EstadoCivil.NAO_INFORMADO, "170", "70"));
        PacienteDTO dto = pacientes.buscarPorId(paciente.getIdPaciente());
        dto.setNome("Simulado editado");
        CountDownLatch aguardandoCaso = new CountDownLatch(1);
        com.SistemaApiCrud.SistemaCrud.service.CasoClinicoLockService spy =
                org.springframework.test.util.AopTestUtils.getUltimateTargetObject(locks);
        org.mockito.Mockito.doAnswer(invocation -> {
            aguardandoCaso.countDown();
            return invocation.callRealMethod();
        }).when(spy).bloquearRascunhos(org.mockito.ArgumentMatchers.anyCollection());
        var tx = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        try (var executor = Executors.newSingleThreadExecutor()) {
            var edicao = tx.execute(status -> {
                casoRepository.findByIdForUpdate(caso.getIdCaso()).orElseThrow();
                var tarefa = executor.submit(() -> {
                    autenticar();
                    try {
                        return pacientes.atualizar(paciente.getIdPaciente(), dto);
                    } finally {
                        limpar();
                    }
                });
                try {
                    assertThat(aguardandoCaso.await(5, TimeUnit.SECONDS)).isTrue();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ex);
                }
                // A exclusao pode obter o filho enquanto a edicao espera no pai.
                assertThat(pacienteRepository.findByIdForUpdate(paciente.getIdPaciente())).isPresent();
                return tarefa;
            });
            assertThat(edicao.get(10, TimeUnit.SECONDS).getNome()).isEqualTo("Simulado editado");
        }
        casos.deletar(caso.getIdCaso());
    }

    private PerguntaRequestDTO pergunta(String texto) {
        PerguntaRequestDTO dto = new PerguntaRequestDTO();
        dto.setTexto(texto);
        dto.setResposta("Explicacao da resposta");
        dto.setTipo(TipoPergunta.VERDADEIRO_FALSO);
        dto.setGabarito("VERDADEIRO");
        return dto;
    }

    private CasoClinico caso(StatusCasoClinico status) {
        Professor professor = professorRepository.save(new Professor(null, "Professor teste",
                UUID.randomUUID() + "@example.com", "Clinica"));
        CasoClinico caso = new CasoClinico();
        caso.setProfessor(professor);
        caso.setTitulo("Caso de teste");
        caso.setDisciplina("Semiologia");
        caso.setAreaSaude("Medicina");
        caso.setEspecialidade("Cardiologia");
        caso.setEstilo("Didatico");
        caso.setNivelDificuldade(NivelDificuldade.MEDIA);
        caso.setStatus(status);
        caso.setTempoLimiteMinutos(60);
        return casoRepository.saveAndFlush(caso);
    }
}
