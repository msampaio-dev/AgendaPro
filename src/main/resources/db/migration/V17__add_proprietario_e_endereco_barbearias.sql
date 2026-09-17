ALTER TABLE barbearias
    ADD COLUMN proprietario_profissional_id BIGINT,
    ADD COLUMN cep VARCHAR(8),
    ADD COLUMN logradouro VARCHAR(160),
    ADD COLUMN numero VARCHAR(20),
    ADD COLUMN complemento VARCHAR(80),
    ADD COLUMN bairro VARCHAR(100),
    ADD COLUMN cidade VARCHAR(100),
    ADD COLUMN estado VARCHAR(2),
    ADD COLUMN fuso_horario VARCHAR(50) NOT NULL DEFAULT 'America/Sao_Paulo';

ALTER TABLE barbearias
ADD CONSTRAINT fk_barbearias_proprietario
FOREIGN KEY (proprietario_profissional_id) REFERENCES profissionais (id);

CREATE INDEX ix_barbearias_proprietario
ON barbearias (proprietario_profissional_id);

ALTER TABLE barbearias
ADD CONSTRAINT ck_barbearias_cep
CHECK (cep IS NULL OR cep ~ '^[0-9]{8}$');

ALTER TABLE barbearias
ADD CONSTRAINT ck_barbearias_estado
CHECK (estado IS NULL OR estado ~ '^[A-Z]{2}$');
