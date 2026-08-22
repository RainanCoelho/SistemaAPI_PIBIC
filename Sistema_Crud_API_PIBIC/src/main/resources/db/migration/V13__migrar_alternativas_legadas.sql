-- Converte as cinco colunas historicas em registros normalizados antes de
-- qualquer remocao estrutural. Registros ja normalizados sempre prevalecem.
INSERT INTO alternativa_pergunta (fk_id_pergunta, letra, texto, correta)
SELECT
    p.id,
    'A',
    p.alternativa_a,
    CASE
        WHEN UPPER(TRIM(COALESCE(p.gabarito, ''))) = 'A'
          OR UPPER(TRIM(COALESCE(p.resposta, ''))) = 'A'
          OR UPPER(TRIM(COALESCE(p.gabarito, ''))) = UPPER(TRIM(p.alternativa_a))
          OR UPPER(TRIM(COALESCE(p.resposta, ''))) = UPPER(TRIM(p.alternativa_a))
        THEN TRUE
        ELSE FALSE
    END
FROM pergunta p
WHERE p.alternativa_a IS NOT NULL
  AND TRIM(p.alternativa_a) <> ''
  AND NOT EXISTS (
      SELECT 1
      FROM alternativa_pergunta a
      WHERE a.fk_id_pergunta = p.id
        AND a.letra = 'A'
  );

INSERT INTO alternativa_pergunta (fk_id_pergunta, letra, texto, correta)
SELECT
    p.id,
    'B',
    p.alternativa_b,
    CASE
        WHEN UPPER(TRIM(COALESCE(p.gabarito, ''))) = 'B'
          OR UPPER(TRIM(COALESCE(p.resposta, ''))) = 'B'
          OR UPPER(TRIM(COALESCE(p.gabarito, ''))) = UPPER(TRIM(p.alternativa_b))
          OR UPPER(TRIM(COALESCE(p.resposta, ''))) = UPPER(TRIM(p.alternativa_b))
        THEN TRUE
        ELSE FALSE
    END
FROM pergunta p
WHERE p.alternativa_b IS NOT NULL
  AND TRIM(p.alternativa_b) <> ''
  AND NOT EXISTS (
      SELECT 1
      FROM alternativa_pergunta a
      WHERE a.fk_id_pergunta = p.id
        AND a.letra = 'B'
  );

INSERT INTO alternativa_pergunta (fk_id_pergunta, letra, texto, correta)
SELECT
    p.id,
    'C',
    p.alternativa_c,
    CASE
        WHEN UPPER(TRIM(COALESCE(p.gabarito, ''))) = 'C'
          OR UPPER(TRIM(COALESCE(p.resposta, ''))) = 'C'
          OR UPPER(TRIM(COALESCE(p.gabarito, ''))) = UPPER(TRIM(p.alternativa_c))
          OR UPPER(TRIM(COALESCE(p.resposta, ''))) = UPPER(TRIM(p.alternativa_c))
        THEN TRUE
        ELSE FALSE
    END
FROM pergunta p
WHERE p.alternativa_c IS NOT NULL
  AND TRIM(p.alternativa_c) <> ''
  AND NOT EXISTS (
      SELECT 1
      FROM alternativa_pergunta a
      WHERE a.fk_id_pergunta = p.id
        AND a.letra = 'C'
  );

INSERT INTO alternativa_pergunta (fk_id_pergunta, letra, texto, correta)
SELECT
    p.id,
    'D',
    p.alternativa_d,
    CASE
        WHEN UPPER(TRIM(COALESCE(p.gabarito, ''))) = 'D'
          OR UPPER(TRIM(COALESCE(p.resposta, ''))) = 'D'
          OR UPPER(TRIM(COALESCE(p.gabarito, ''))) = UPPER(TRIM(p.alternativa_d))
          OR UPPER(TRIM(COALESCE(p.resposta, ''))) = UPPER(TRIM(p.alternativa_d))
        THEN TRUE
        ELSE FALSE
    END
FROM pergunta p
WHERE p.alternativa_d IS NOT NULL
  AND TRIM(p.alternativa_d) <> ''
  AND NOT EXISTS (
      SELECT 1
      FROM alternativa_pergunta a
      WHERE a.fk_id_pergunta = p.id
        AND a.letra = 'D'
  );

INSERT INTO alternativa_pergunta (fk_id_pergunta, letra, texto, correta)
SELECT
    p.id,
    'E',
    p.alternativa_e,
    CASE
        WHEN UPPER(TRIM(COALESCE(p.gabarito, ''))) = 'E'
          OR UPPER(TRIM(COALESCE(p.resposta, ''))) = 'E'
          OR UPPER(TRIM(COALESCE(p.gabarito, ''))) = UPPER(TRIM(p.alternativa_e))
          OR UPPER(TRIM(COALESCE(p.resposta, ''))) = UPPER(TRIM(p.alternativa_e))
        THEN TRUE
        ELSE FALSE
    END
FROM pergunta p
WHERE p.alternativa_e IS NOT NULL
  AND TRIM(p.alternativa_e) <> ''
  AND NOT EXISTS (
      SELECT 1
      FROM alternativa_pergunta a
      WHERE a.fk_id_pergunta = p.id
        AND a.letra = 'E'
  );

-- Interrompe a migracao, sem apagar as colunas de origem, se alguma pergunta
-- de multipla escolha ainda nao possuir ao menos duas opcoes e exatamente uma
-- correta. Assim uma inconsistencia historica exige reparo explicito.
CREATE TABLE validacao_migracao_v13 (
    id INTEGER PRIMARY KEY,
    quantidade_inconsistencias BIGINT NOT NULL,
    CONSTRAINT ck_validacao_migracao_v13
        CHECK (quantidade_inconsistencias = 0)
);

INSERT INTO validacao_migracao_v13 (id, quantidade_inconsistencias)
SELECT 1, COUNT(*)
FROM (
    SELECT p.id
    FROM pergunta p
    LEFT JOIN alternativa_pergunta a ON a.fk_id_pergunta = p.id
    WHERE UPPER(TRIM(p.tipo)) = 'MULTIPLA_ESCOLHA'
    GROUP BY p.id
    HAVING COUNT(a.id_alternativa) < 2
        OR SUM(CASE WHEN a.correta = TRUE THEN 1 ELSE 0 END) <> 1
) inconsistencias;

DROP TABLE validacao_migracao_v13;
