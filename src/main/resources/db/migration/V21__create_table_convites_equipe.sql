CREATE TABLE convites_equipe (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    barbearia_id BIGINT NOT NULL,
    email VARCHAR(254) NOT NULL,
    status VARCHAR(15) NOT NULL,
    criado_por_usuario_id BIGINT NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL,
    expira_em TIMESTAMPTZ NOT NULL,
    respondido_em TIMESTAMPTZ,

    CONSTRAINT pk_convites_equipe PRIMARY KEY (id),
    CONSTRAINT fk_convites_equipe_barbearia
        FOREIGN KEY (barbearia_id) REFERENCES barbearias (id),
    CONSTRAINT fk_convites_equipe_criador
        FOREIGN KEY (criado_por_usuario_id) REFERENCES usuarios (id),
    CONSTRAINT ck_convites_equipe_status
        CHECK (status IN ('PENDENTE', 'ACEITO', 'RECUSADO', 'CANCELADO', 'EXPIRADO')),
    CONSTRAINT ck_convites_equipe_expiracao
        CHECK (expira_em > criado_em)
);

CREATE UNIQUE INDEX uk_convites_equipe_pendente
ON convites_equipe (barbearia_id, LOWER(email))
WHERE status = 'PENDENTE';

CREATE INDEX ix_convites_equipe_email_status
ON convites_equipe (LOWER(email), status);

CREATE INDEX ix_convites_equipe_barbearia
ON convites_equipe (barbearia_id, criado_em DESC);
