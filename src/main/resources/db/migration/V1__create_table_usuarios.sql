CREATE TABLE usuarios (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    nome VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,

    CONSTRAINT pk_usuarios PRIMARY KEY (id),
    CONSTRAINT uk_usuarios_email UNIQUE (email)
);