ALTER TABLE revisao_resposta_aluno DROP CONSTRAINT fk_revisao_resposta;
ALTER TABLE revisao_resposta_aluno
    ADD CONSTRAINT fk_revisao_resposta
    FOREIGN KEY (fk_id_resposta) REFERENCES resposta_aluno (id) ON DELETE CASCADE;

ALTER TABLE resposta_aluno DROP CONSTRAINT fk_resposta_pergunta_caso;
ALTER TABLE resposta_aluno DROP CONSTRAINT fk_resposta_pergunta;
ALTER TABLE resposta_aluno DROP CONSTRAINT fk_resposta_caso;
ALTER TABLE resposta_aluno
    ADD CONSTRAINT fk_resposta_caso
    FOREIGN KEY (fk_id_caso) REFERENCES casos_clinicos (id_caso) ON DELETE CASCADE;
ALTER TABLE resposta_aluno
    ADD CONSTRAINT fk_resposta_pergunta
    FOREIGN KEY (fk_id_pergunta) REFERENCES pergunta (id) ON DELETE CASCADE;
ALTER TABLE resposta_aluno
    ADD CONSTRAINT fk_resposta_pergunta_caso
    FOREIGN KEY (fk_id_pergunta, fk_id_caso)
    REFERENCES pergunta (id, fk_id_caso) ON DELETE CASCADE;

ALTER TABLE alternativa_pergunta DROP CONSTRAINT fk_alternativa_pergunta;
ALTER TABLE alternativa_pergunta
    ADD CONSTRAINT fk_alternativa_pergunta
    FOREIGN KEY (fk_id_pergunta) REFERENCES pergunta (id) ON DELETE CASCADE;

ALTER TABLE paciente DROP CONSTRAINT fk_paciente_caso;
ALTER TABLE paciente
    ADD CONSTRAINT fk_paciente_caso
    FOREIGN KEY (fk_id_caso) REFERENCES casos_clinicos (id_caso) ON DELETE CASCADE;

ALTER TABLE conteudo_clinico DROP CONSTRAINT fk_conteudo_caso;
ALTER TABLE conteudo_clinico
    ADD CONSTRAINT fk_conteudo_caso
    FOREIGN KEY (fk_id_caso) REFERENCES casos_clinicos (id_caso) ON DELETE CASCADE;

ALTER TABLE pergunta DROP CONSTRAINT fk_pergunta_caso;
ALTER TABLE pergunta
    ADD CONSTRAINT fk_pergunta_caso
    FOREIGN KEY (fk_id_caso) REFERENCES casos_clinicos (id_caso) ON DELETE CASCADE;

ALTER TABLE tentativa_caso DROP CONSTRAINT fk_tentativa_caso;
ALTER TABLE tentativa_caso
    ADD CONSTRAINT fk_tentativa_caso
    FOREIGN KEY (fk_id_caso) REFERENCES casos_clinicos (id_caso) ON DELETE CASCADE;

ALTER TABLE auditoria_geracao_ia DROP CONSTRAINT fk_auditoria_ia_caso;
ALTER TABLE auditoria_geracao_ia
    ADD CONSTRAINT fk_auditoria_ia_caso
    FOREIGN KEY (fk_id_caso) REFERENCES casos_clinicos (id_caso) ON DELETE CASCADE;
