package com.SistemaApiCrud.SistemaCrud;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import com.SistemaApiCrud.SistemaCrud.entity.AlternativaPergunta;
import com.SistemaApiCrud.SistemaCrud.entity.AuditoriaGeracaoIa;
import com.SistemaApiCrud.SistemaCrud.entity.Professor;
import com.SistemaApiCrud.SistemaCrud.entity.RespostaAluno;
import com.SistemaApiCrud.SistemaCrud.entity.RevisaoRespostaAluno;
import com.SistemaApiCrud.SistemaCrud.entity.TentativaCaso;
import com.SistemaApiCrud.SistemaCrud.entity.Usuario;
import com.SistemaApiCrud.SistemaCrud.entity.CasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.ConteudoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.enums.NivelDificuldade;
import com.SistemaApiCrud.SistemaCrud.entity.enums.StatusCasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.Paciente;
import com.SistemaApiCrud.SistemaCrud.entity.Pergunta;
import com.SistemaApiCrud.SistemaCrud.repository.CasoClinicoRepository;
import com.SistemaApiCrud.SistemaCrud.repository.ProfessorRepository;
import com.SistemaApiCrud.SistemaCrud.service.CasoClinicoService;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.open-in-view=false"
})
class JpaFetchPlanTests {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private CasoClinicoRepository casoRepository;

    @Autowired
    private CasoClinicoService casoService;

    @Test
    void todasAsAssociacoesDeEntidadeDevemSerLazy() throws Exception {
        List<Associacao> associacoes = List.of(
                new Associacao(AlternativaPergunta.class, "pergunta", true),
                new Associacao(AuditoriaGeracaoIa.class, "casoClinico", true),
                new Associacao(AuditoriaGeracaoIa.class, "usuario", true),
                new Associacao(CasoClinico.class, "professor", true),
                new Associacao(ConteudoClinico.class, "casoClinico", true),
                new Associacao(Paciente.class, "casoClinico", true),
                new Associacao(Pergunta.class, "casoClinico", true),
                new Associacao(RespostaAluno.class, "aluno", true),
                new Associacao(RespostaAluno.class, "casoClinico", true),
                new Associacao(RespostaAluno.class, "pergunta", true),
                new Associacao(RespostaAluno.class, "revisor", false),
                new Associacao(RevisaoRespostaAluno.class, "resposta", true),
                new Associacao(RevisaoRespostaAluno.class, "revisor", true),
                new Associacao(TentativaCaso.class, "aluno", true),
                new Associacao(TentativaCaso.class, "casoClinico", true));

        for (Associacao associacao : associacoes) {
            Field campo = associacao.tipo().getDeclaredField(associacao.campo());
            ManyToOne manyToOne = campo.getAnnotation(ManyToOne.class);
            assertThat(manyToOne)
                    .as("%s.%s deve declarar @ManyToOne", associacao.tipo().getSimpleName(), associacao.campo())
                    .isNotNull();
            assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
            assertThat(manyToOne.optional()).isEqualTo(!associacao.obrigatoria());
        }

        assertOneToOneLazy(Usuario.class, "aluno");
        assertOneToOneLazy(Usuario.class, "professor");
    }

    @Test
    void listagemDeCasosNaoDeveBuscarUmProfessorPorItem() {
        String marcador = UUID.randomUUID().toString();
        Professor professor = professorRepository.saveAndFlush(new Professor(
                null,
                "Professora Fetch " + marcador,
                "fetch-" + marcador + "@example.com",
                "Clinica"));

        for (int indice = 1; indice <= 3; indice++) {
            CasoClinico caso = new CasoClinico();
            caso.setProfessor(professor);
            caso.setTitulo("Caso para validar fetch " + indice);
            caso.setNivelDificuldade(NivelDificuldade.MEDIA);
            caso.setDisciplina("Clinica");
            caso.setAreaSaude("Medicina");
            caso.setEstilo("Raciocinio clinico");
            caso.setEspecialidade("Clinica medica");
            caso.setStatus(StatusCasoClinico.RASCUNHO);
            caso.setTempoLimiteMinutos(60);
            casoRepository.save(caso);
        }
        casoRepository.flush();

        Statistics estatisticas = entityManagerFactory
                .unwrap(SessionFactory.class)
                .getStatistics();
        estatisticas.clear();

        var pagina = casoService.listarPorProfessor(
                professor.getId(),
                PageRequest.of(0, 20));

        assertThat(pagina.getTotalElements()).isEqualTo(3);
        assertThat(pagina.getContent())
                .hasSize(3)
                .allSatisfy(caso -> assertThat(caso.getIdProfessor())
                        .isEqualTo(professor.getId()));
        assertThat(estatisticas.getEntityFetchCount())
                .as("nenhuma associacao deve ser buscada individualmente")
                .isZero();
        assertThat(estatisticas.getPrepareStatementCount())
                .as("uma consulta de existencia e uma consulta de pagina sao suficientes")
                .isLessThanOrEqualTo(2);
    }

    private void assertOneToOneLazy(Class<?> tipo, String nomeCampo) throws Exception {
        Field campo = tipo.getDeclaredField(nomeCampo);
        OneToOne oneToOne = campo.getAnnotation(OneToOne.class);
        assertThat(oneToOne)
                .as("%s.%s deve declarar @OneToOne", tipo.getSimpleName(), nomeCampo)
                .isNotNull();
        assertThat(oneToOne.fetch()).isEqualTo(FetchType.LAZY);
    }

    private record Associacao(Class<?> tipo, String campo, boolean obrigatoria) {
    }
}
