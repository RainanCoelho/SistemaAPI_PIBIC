ALTER TABLE resposta_aluno ALTER COLUMN correta DROP NOT NULL;

UPDATE resposta_aluno
SET correta = NULL
WHERE fk_id_pergunta IN (
    SELECT id
    FROM pergunta
    WHERE tipo IN ('DISCURSIVA', 'CONDUTA_CLINICA')
);
