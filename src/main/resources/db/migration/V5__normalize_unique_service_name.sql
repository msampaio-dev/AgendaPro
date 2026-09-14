ALTER TABLE servicos
DROP CONSTRAINT uk_servicos_nome;

CREATE UNIQUE INDEX uk_servicos_nome_normalizado
ON servicos (LOWER(BTRIM(nome)));
