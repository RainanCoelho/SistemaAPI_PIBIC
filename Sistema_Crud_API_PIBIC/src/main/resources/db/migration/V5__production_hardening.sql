ALTER TABLE usuario ADD COLUMN versao_credencial BIGINT NOT NULL DEFAULT 0;
ALTER TABLE usuario ALTER COLUMN username TYPE VARCHAR(100);
ALTER TABLE usuario ALTER COLUMN role TYPE VARCHAR(30);

ALTER TABLE aluno ALTER COLUMN nome TYPE VARCHAR(150);
ALTER TABLE aluno ALTER COLUMN email TYPE VARCHAR(254);
ALTER TABLE aluno ALTER COLUMN curso TYPE VARCHAR(120);
ALTER TABLE aluno ALTER COLUMN periodo TYPE VARCHAR(20);
ALTER TABLE aluno ALTER COLUMN nome SET NOT NULL;
ALTER TABLE aluno ALTER COLUMN email SET NOT NULL;
ALTER TABLE aluno ALTER COLUMN curso SET NOT NULL;
ALTER TABLE aluno ALTER COLUMN periodo SET NOT NULL;

ALTER TABLE professor ALTER COLUMN nome TYPE VARCHAR(150);
ALTER TABLE professor ALTER COLUMN email TYPE VARCHAR(254);
ALTER TABLE professor ALTER COLUMN materia TYPE VARCHAR(120);
ALTER TABLE professor ALTER COLUMN nome SET NOT NULL;
ALTER TABLE professor ALTER COLUMN email SET NOT NULL;
ALTER TABLE professor ALTER COLUMN materia SET NOT NULL;

ALTER TABLE casos_clinicos ALTER COLUMN titulo TYPE VARCHAR(200);
ALTER TABLE casos_clinicos ALTER COLUMN dificuldade TYPE VARCHAR(30);
ALTER TABLE casos_clinicos ALTER COLUMN disciplina TYPE VARCHAR(120);
ALTER TABLE casos_clinicos ALTER COLUMN area_saude TYPE VARCHAR(120);
ALTER TABLE casos_clinicos ALTER COLUMN estilo TYPE VARCHAR(60);
ALTER TABLE casos_clinicos ALTER COLUMN especialidade TYPE VARCHAR(120);
ALTER TABLE casos_clinicos ALTER COLUMN status TYPE VARCHAR(30);
ALTER TABLE casos_clinicos ALTER COLUMN objetivo_aprendizagem TYPE TEXT;
ALTER TABLE casos_clinicos ALTER COLUMN nivel_dificuldade TYPE VARCHAR(30);
ALTER TABLE casos_clinicos ALTER COLUMN fk_id_professor SET NOT NULL;
ALTER TABLE casos_clinicos ALTER COLUMN titulo SET NOT NULL;
ALTER TABLE casos_clinicos ALTER COLUMN dificuldade SET NOT NULL;
ALTER TABLE casos_clinicos ALTER COLUMN disciplina SET NOT NULL;
ALTER TABLE casos_clinicos ALTER COLUMN area_saude SET NOT NULL;
ALTER TABLE casos_clinicos ALTER COLUMN estilo SET NOT NULL;
ALTER TABLE casos_clinicos ALTER COLUMN especialidade SET NOT NULL;
ALTER TABLE casos_clinicos ALTER COLUMN status SET NOT NULL;
ALTER TABLE casos_clinicos ALTER COLUMN data_criacao SET NOT NULL;
ALTER TABLE casos_clinicos ALTER COLUMN data_atualizacao SET NOT NULL;

ALTER TABLE paciente ALTER COLUMN nome TYPE VARCHAR(150);
ALTER TABLE paciente ALTER COLUMN profissao TYPE VARCHAR(120);
ALTER TABLE paciente ALTER COLUMN sexo TYPE VARCHAR(30);
ALTER TABLE paciente ALTER COLUMN estado_civil TYPE VARCHAR(30);
ALTER TABLE paciente ALTER COLUMN altura TYPE VARCHAR(20);
ALTER TABLE paciente ALTER COLUMN peso TYPE VARCHAR(20);
ALTER TABLE paciente ALTER COLUMN fk_id_caso SET NOT NULL;
ALTER TABLE paciente ALTER COLUMN nome SET NOT NULL;
ALTER TABLE paciente ALTER COLUMN profissao SET NOT NULL;
ALTER TABLE paciente ALTER COLUMN sexo SET NOT NULL;
ALTER TABLE paciente ALTER COLUMN idade SET NOT NULL;
ALTER TABLE paciente ALTER COLUMN estado_civil SET NOT NULL;
ALTER TABLE paciente ALTER COLUMN altura SET NOT NULL;
ALTER TABLE paciente ALTER COLUMN peso SET NOT NULL;

ALTER TABLE conteudo_clinico ALTER COLUMN sintomas TYPE TEXT;
ALTER TABLE conteudo_clinico ALTER COLUMN contexto TYPE TEXT;
ALTER TABLE conteudo_clinico ALTER COLUMN exam_clinico TYPE TEXT;
ALTER TABLE conteudo_clinico ALTER COLUMN antec_clinico TYPE TEXT;
ALTER TABLE conteudo_clinico ALTER COLUMN diag_esperado TYPE TEXT;
ALTER TABLE conteudo_clinico ALTER COLUMN fk_id_caso SET NOT NULL;
ALTER TABLE conteudo_clinico ALTER COLUMN sintomas SET NOT NULL;
ALTER TABLE conteudo_clinico ALTER COLUMN contexto SET NOT NULL;
ALTER TABLE conteudo_clinico ALTER COLUMN exam_clinico SET NOT NULL;
ALTER TABLE conteudo_clinico ALTER COLUMN antec_clinico SET NOT NULL;
ALTER TABLE conteudo_clinico ALTER COLUMN diag_esperado SET NOT NULL;

ALTER TABLE pergunta ALTER COLUMN texto TYPE TEXT;
ALTER TABLE pergunta ALTER COLUMN alternativa_a TYPE TEXT;
ALTER TABLE pergunta ALTER COLUMN alternativa_b TYPE TEXT;
ALTER TABLE pergunta ALTER COLUMN alternativa_c TYPE TEXT;
ALTER TABLE pergunta ALTER COLUMN alternativa_d TYPE TEXT;
ALTER TABLE pergunta ALTER COLUMN alternativa_e TYPE TEXT;
ALTER TABLE pergunta ALTER COLUMN resposta TYPE TEXT;
ALTER TABLE pergunta ALTER COLUMN tipo TYPE VARCHAR(40);
ALTER TABLE pergunta ALTER COLUMN gabarito TYPE TEXT;
ALTER TABLE pergunta ALTER COLUMN tempo_esperado TYPE VARCHAR(50);
ALTER TABLE pergunta ALTER COLUMN fk_id_caso SET NOT NULL;
ALTER TABLE pergunta ALTER COLUMN texto SET NOT NULL;
ALTER TABLE pergunta ALTER COLUMN resposta SET NOT NULL;
ALTER TABLE pergunta ALTER COLUMN tipo SET NOT NULL;
ALTER TABLE pergunta ALTER COLUMN gabarito SET NOT NULL;
ALTER TABLE pergunta ALTER COLUMN tempo_esperado SET NOT NULL;

ALTER TABLE alternativa_pergunta ALTER COLUMN letra TYPE VARCHAR(10);
ALTER TABLE alternativa_pergunta ALTER COLUMN texto TYPE TEXT;
ALTER TABLE alternativa_pergunta ALTER COLUMN letra SET NOT NULL;
ALTER TABLE alternativa_pergunta ALTER COLUMN texto SET NOT NULL;
ALTER TABLE alternativa_pergunta ALTER COLUMN correta SET NOT NULL;

ALTER TABLE resposta_aluno ALTER COLUMN resposta_marcada TYPE TEXT;
ALTER TABLE resposta_aluno ALTER COLUMN fk_id_aluno SET NOT NULL;
ALTER TABLE resposta_aluno ALTER COLUMN fk_id_caso SET NOT NULL;
ALTER TABLE resposta_aluno ALTER COLUMN fk_id_pergunta SET NOT NULL;
ALTER TABLE resposta_aluno ALTER COLUMN resposta_marcada SET NOT NULL;
ALTER TABLE resposta_aluno ALTER COLUMN correta SET NOT NULL;
ALTER TABLE resposta_aluno ALTER COLUMN data_resposta SET NOT NULL;

ALTER TABLE resposta_aluno
    ADD CONSTRAINT uk_resposta_aluno_caso_pergunta
    UNIQUE (fk_id_aluno, fk_id_caso, fk_id_pergunta);

CREATE INDEX idx_caso_professor ON casos_clinicos (fk_id_professor);
CREATE INDEX idx_caso_status ON casos_clinicos (status);
CREATE INDEX idx_pergunta_caso ON pergunta (fk_id_caso);
CREATE INDEX idx_paciente_caso ON paciente (fk_id_caso);
CREATE INDEX idx_conteudo_caso ON conteudo_clinico (fk_id_caso);
CREATE INDEX idx_resposta_aluno_caso ON resposta_aluno (fk_id_aluno, fk_id_caso);
