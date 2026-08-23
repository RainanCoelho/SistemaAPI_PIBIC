package com.SistemaApiCrud.SistemaCrud.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import com.SistemaApiCrud.SistemaCrud.entity.CasoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.ConteudoClinico;
import com.SistemaApiCrud.SistemaCrud.entity.Paciente;

public final class CasoClinicoFingerprint {

    private CasoClinicoFingerprint() {
    }

    public static String calcular(
            CasoClinico caso,
            ConteudoClinico conteudo,
            List<Paciente> pacientes) {
        StringBuilder dados = new StringBuilder();
        adicionar(dados, caso.getIdCaso());
        adicionar(dados, caso.getProfessor() != null ? caso.getProfessor().getId() : null);
        adicionar(dados, caso.getTitulo());
        adicionar(dados, caso.getDisciplina());
        adicionar(dados, caso.getAreaSaude());
        adicionar(dados, caso.getEspecialidade());
        adicionar(dados, caso.getNivelDificuldade());
        adicionar(dados, caso.getEstilo());
        adicionar(dados, caso.getObjetivoAprendizagem());
        adicionar(dados, conteudo != null);
        adicionar(dados, conteudo != null ? conteudo.getIdConteudo() : null);
        adicionar(dados, conteudo != null ? conteudo.getSintomas() : null);
        adicionar(dados, conteudo != null ? conteudo.getContexto() : null);
        adicionar(dados, conteudo != null ? conteudo.getExamClinico() : null);
        adicionar(dados, conteudo != null ? conteudo.getAntecClinico() : null);
        adicionar(dados, conteudo != null ? conteudo.getDiagEsperado() : null);

        for (Paciente paciente : pacientes) {
            adicionar(dados, paciente.getIdPaciente());
            adicionar(dados, paciente.getNome());
            adicionar(dados, paciente.getIdade());
            adicionar(dados, paciente.getSexo());
            adicionar(dados, paciente.getEstadoCivil());
            adicionar(dados, paciente.getProfissao());
            adicionar(dados, paciente.getPeso());
            adicionar(dados, paciente.getAltura());
        }

        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256")
                    .digest(dados.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumo);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponivel", ex);
        }
    }

    private static void adicionar(StringBuilder dados, Object valor) {
        String texto = valor == null ? "" : valor.toString();
        dados.append(texto.length()).append(':').append(texto).append('|');
    }
}
