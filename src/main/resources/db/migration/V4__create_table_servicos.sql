CREATE TABLE servicos (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    nome VARCHAR(120) NOT NULL,
    descricao VARCHAR(500),
    duracao_minutos INTEGER NOT NULL,
    preco NUMERIC(10, 2) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_servicos
        PRIMARY KEY (id),

    CONSTRAINT uk_servicos_nome
        UNIQUE (nome),

    CONSTRAINT ck_servicos_duracao_positiva
        CHECK (duracao_minutos > 0),

    CONSTRAINT ck_servicos_preco_nao_negativo
        CHECK (preco >= 0)
);
