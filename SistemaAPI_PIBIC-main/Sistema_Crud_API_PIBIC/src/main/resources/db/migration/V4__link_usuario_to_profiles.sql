ALTER TABLE usuario ADD COLUMN fk_id_aluno BIGINT;
ALTER TABLE usuario ADD COLUMN fk_id_professor BIGINT;

ALTER TABLE usuario ADD CONSTRAINT uk_usuario_aluno UNIQUE (fk_id_aluno);
ALTER TABLE usuario ADD CONSTRAINT uk_usuario_professor UNIQUE (fk_id_professor);

ALTER TABLE usuario ADD CONSTRAINT fk_usuario_aluno FOREIGN KEY (fk_id_aluno) REFERENCES aluno (id_aluno);
ALTER TABLE usuario ADD CONSTRAINT fk_usuario_professor FOREIGN KEY (fk_id_professor) REFERENCES professor (id);
