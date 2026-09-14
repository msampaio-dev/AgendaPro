CREATE TABLE profissionais (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    usuario_id BIGINT NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_profissionais
        PRIMARY KEY (id),

    CONSTRAINT fk_profissionais_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id),

    CONSTRAINT uk_profissionais_usuario
        UNIQUE (usuario_id)
);