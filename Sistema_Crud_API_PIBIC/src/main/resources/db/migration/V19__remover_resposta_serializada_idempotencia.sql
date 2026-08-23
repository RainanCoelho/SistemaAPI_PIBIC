-- O replay idempotente usa somente os IDs canonicos gravados desde a V18.
-- Esta copia legada pode conter conteudo clinico e foi autorizada para descarte.
ALTER TABLE solicitacao_geracao_ia
    DROP COLUMN resposta_serializada;
