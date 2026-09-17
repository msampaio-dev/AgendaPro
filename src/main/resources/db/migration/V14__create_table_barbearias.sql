CREATE TABLE barbearias (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    nome VARCHAR(120) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_barbearias PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uk_barbearias_nome_normalizado
ON barbearias (LOWER(nome));

INSERT INTO barbearias (nome) VALUES ('Unidade principal');

ALTER TABLE profissionais ADD COLUMN barbearia_id BIGINT;

UPDATE profissionais
SET barbearia_id = (SELECT id FROM barbearias WHERE nome = 'Unidade principal');

ALTER TABLE profissionais ALTER COLUMN barbearia_id SET NOT NULL;

ALTER TABLE profissionais
ADD CONSTRAINT fk_profissionais_barbearia
FOREIGN KEY (barbearia_id) REFERENCES barbearias (id);

CREATE INDEX ix_profissionais_barbearia ON profissionais (barbearia_id);
