-- Garante que todo nivel historico possui uma conversao sem ambiguidades antes
-- de eliminar o texto livre. Valores desconhecidos interrompem a migracao.
-- O DROP inicial permite nova tentativa segura no H2 depois de corrigir os
-- dados e executar o repair do Flyway (DDL no H2 faz commit implicito).
DROP TABLE IF EXISTS validacao_migracao_v14;

CREATE TABLE validacao_migracao_v14 (
    id INTEGER PRIMARY KEY,
    quantidade_inconsistencias BIGINT NOT NULL,
    CONSTRAINT ck_validacao_migracao_v14
        CHECK (quantidade_inconsistencias = 0)
);

INSERT INTO validacao_migracao_v14 (id, quantidade_inconsistencias)
SELECT 1, COUNT(*)
FROM casos_clinicos
WHERE UPPER(TRIM(COALESCE(nivel_dificuldade, ''))) NOT IN (
          'BAIXA', 'MEDIA', 'ALTA'
      )
  AND UPPER(TRIM(COALESCE(dificuldade, ''))) NOT IN (
          'BAIXA', 'BAIXO', 'FACIL', 'FÁCIL', 'EASY',
          'MEDIA', 'MÉDIA', 'MEDIO', 'MÉDIO', 'MODERADA', 'MODERADO', 'MEDIUM',
          'ALTA', 'ALTO', 'DIFICIL', 'DIFÍCIL', 'HARD'
      );

DROP TABLE validacao_migracao_v14;

UPDATE casos_clinicos
SET nivel_dificuldade = CASE
    WHEN UPPER(TRIM(COALESCE(nivel_dificuldade, ''))) IN ('BAIXA', 'MEDIA', 'ALTA')
        THEN UPPER(TRIM(nivel_dificuldade))
    WHEN UPPER(TRIM(dificuldade)) IN ('BAIXA', 'BAIXO', 'FACIL', 'FÁCIL', 'EASY')
        THEN 'BAIXA'
    WHEN UPPER(TRIM(dificuldade)) IN (
            'MEDIA', 'MÉDIA', 'MEDIO', 'MÉDIO', 'MODERADA', 'MODERADO', 'MEDIUM'
        )
        THEN 'MEDIA'
    WHEN UPPER(TRIM(dificuldade)) IN ('ALTA', 'ALTO', 'DIFICIL', 'DIFÍCIL', 'HARD')
        THEN 'ALTA'
END;

ALTER TABLE casos_clinicos
    ALTER COLUMN nivel_dificuldade SET NOT NULL;

ALTER TABLE casos_clinicos
    ADD CONSTRAINT ck_caso_nivel_dificuldade
    CHECK (nivel_dificuldade IN ('BAIXA', 'MEDIA', 'ALTA'));

ALTER TABLE pergunta DROP COLUMN alternativa_a;
ALTER TABLE pergunta DROP COLUMN alternativa_b;
ALTER TABLE pergunta DROP COLUMN alternativa_c;
ALTER TABLE pergunta DROP COLUMN alternativa_d;
ALTER TABLE pergunta DROP COLUMN alternativa_e;

ALTER TABLE casos_clinicos DROP COLUMN dificuldade;
