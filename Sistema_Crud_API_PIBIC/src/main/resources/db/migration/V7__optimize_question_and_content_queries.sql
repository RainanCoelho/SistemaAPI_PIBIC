DROP INDEX idx_conteudo_caso;
DROP INDEX idx_resposta_aluno_caso;
DROP INDEX idx_tentativa_caso_aluno;

CREATE INDEX idx_alternativa_pergunta_pergunta_letra
    ON alternativa_pergunta (fk_id_pergunta, letra);

CREATE INDEX idx_conteudo_caso_recente
    ON conteudo_clinico (fk_id_caso, id_conteudo DESC);

CREATE INDEX idx_resposta_caso
    ON resposta_aluno (fk_id_caso, correta, data_resposta);

CREATE INDEX idx_resposta_aluno_data
    ON resposta_aluno (fk_id_aluno, data_resposta DESC);
