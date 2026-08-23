ALTER TABLE auditoria_geracao_ia
    ADD COLUMN duracao_provedor_ms BIGINT;

ALTER TABLE auditoria_geracao_ia
    ADD COLUMN tokens_entrada INTEGER;

ALTER TABLE auditoria_geracao_ia
    ADD COLUMN tokens_saida INTEGER;

ALTER TABLE auditoria_geracao_ia
    ADD COLUMN correlation_id VARCHAR(64);

ALTER TABLE auditoria_geracao_ia
    ADD CONSTRAINT ck_auditoria_ia_duracao_provedor
        CHECK (duracao_provedor_ms IS NULL OR duracao_provedor_ms >= 0);

ALTER TABLE auditoria_geracao_ia
    ADD CONSTRAINT ck_auditoria_ia_tokens_entrada
        CHECK (tokens_entrada IS NULL OR tokens_entrada >= 0);

ALTER TABLE auditoria_geracao_ia
    ADD CONSTRAINT ck_auditoria_ia_tokens_saida
        CHECK (tokens_saida IS NULL OR tokens_saida >= 0);
