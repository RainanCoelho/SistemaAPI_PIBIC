ALTER TABLE alternativa_pergunta
    ADD CONSTRAINT uk_alternativa_pergunta_letra
    UNIQUE (fk_id_pergunta, letra);

ALTER TABLE alternativa_pergunta
    ADD CONSTRAINT ck_alternativa_pergunta_letra
    CHECK (letra IN ('A', 'B', 'C', 'D', 'E'));

ALTER TABLE pergunta
    ADD CONSTRAINT uk_pergunta_id_caso
    UNIQUE (id, fk_id_caso);

ALTER TABLE resposta_aluno
    ADD CONSTRAINT fk_resposta_pergunta_caso
    FOREIGN KEY (fk_id_pergunta, fk_id_caso)
    REFERENCES pergunta (id, fk_id_caso);

ALTER TABLE paciente
    ADD CONSTRAINT ck_paciente_idade
    CHECK (idade BETWEEN 0 AND 130);

ALTER TABLE resposta_aluno
    ALTER COLUMN data_resposta SET DATA TYPE TIMESTAMP WITH TIME ZONE;

ALTER TABLE casos_clinicos ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;
ALTER TABLE paciente ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;
ALTER TABLE conteudo_clinico ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;
ALTER TABLE pergunta ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;
ALTER TABLE resposta_aluno ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;
