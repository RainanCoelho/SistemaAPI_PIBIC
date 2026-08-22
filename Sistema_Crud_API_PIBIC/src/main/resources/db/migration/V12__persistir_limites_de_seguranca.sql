CREATE TABLE coordenacao_tentativa_login (
    slot SMALLINT PRIMARY KEY,
    CONSTRAINT ck_coordenacao_login_slot CHECK (slot BETWEEN 0 AND 31)
);

INSERT INTO coordenacao_tentativa_login (slot) VALUES
    (0), (1), (2), (3), (4), (5), (6), (7),
    (8), (9), (10), (11), (12), (13), (14), (15),
    (16), (17), (18), (19), (20), (21), (22), (23),
    (24), (25), (26), (27), (28), (29), (30), (31);

CREATE TABLE tentativa_login (
    tipo VARCHAR(10) NOT NULL,
    identificador_hash VARCHAR(64) NOT NULL,
    quantidade INTEGER NOT NULL,
    ultima_tentativa TIMESTAMP WITH TIME ZONE NOT NULL,
    bloqueado_ate TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (tipo, identificador_hash),
    CONSTRAINT ck_tentativa_login_tipo CHECK (tipo IN ('CONTA', 'IP')),
    CONSTRAINT ck_tentativa_login_quantidade CHECK (quantidade > 0)
);

CREATE INDEX idx_tentativa_login_expiracao
    ON tentativa_login (ultima_tentativa, bloqueado_ate);

CREATE TABLE coordenacao_uso_ia (
    slot SMALLINT PRIMARY KEY,
    CONSTRAINT ck_coordenacao_uso_ia_slot CHECK (slot BETWEEN 0 AND 32)
);

INSERT INTO coordenacao_uso_ia (slot) VALUES
    (0), (1), (2), (3), (4), (5), (6), (7),
    (8), (9), (10), (11), (12), (13), (14), (15),
    (16), (17), (18), (19), (20), (21), (22), (23),
    (24), (25), (26), (27), (28), (29), (30), (31), (32);

CREATE TABLE cota_uso_ia (
    identificador_hash VARCHAR(64) PRIMARY KEY,
    minuto_inicio TIMESTAMP WITH TIME ZONE NOT NULL,
    usos_minuto INTEGER NOT NULL,
    dia DATE NOT NULL,
    usos_dia INTEGER NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_cota_ia_usos_minuto CHECK (usos_minuto > 0),
    CONSTRAINT ck_cota_ia_usos_dia CHECK (usos_dia > 0)
);

CREATE INDEX idx_cota_uso_ia_atualizacao ON cota_uso_ia (atualizado_em);

CREATE TABLE lease_uso_ia (
    id VARCHAR(36) PRIMARY KEY,
    identificador_hash VARCHAR(64) NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    expira_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_lease_ia_periodo CHECK (expira_em > criado_em)
);

CREATE INDEX idx_lease_uso_ia_expiracao ON lease_uso_ia (expira_em);
