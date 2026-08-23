ALTER TABLE solicitacao_geracao_ia
    ALTER COLUMN fk_id_caso DROP NOT NULL;

ALTER TABLE solicitacao_geracao_ia
    DROP CONSTRAINT ck_solicitacao_ia_concluida;

ALTER TABLE solicitacao_geracao_ia
    ADD COLUMN ids_resultado VARCHAR(1000);

ALTER TABLE solicitacao_geracao_ia
    ADD COLUMN expira_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE solicitacao_geracao_ia
    ADD CONSTRAINT fk_solicitacao_ia_caso
        FOREIGN KEY (fk_id_caso) REFERENCES casos_clinicos (id_caso) ON DELETE SET NULL;

ALTER TABLE solicitacao_geracao_ia
    ADD CONSTRAINT ck_solicitacao_ia_concluida_metadados
        CHECK (estado <> 'CONCLUIDA' OR (status_resposta IS NOT NULL AND ids_resultado IS NOT NULL));

CREATE INDEX idx_solicitacao_ia_expiracao ON solicitacao_geracao_ia (expira_em);
