CREATE TABLE profissionais_servicos (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    profissional_id BIGINT NOT NULL,
    servico_id BIGINT NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_profissionais_servicos
        PRIMARY KEY (id),

    CONSTRAINT fk_profissionais_servicos_profissional
        FOREIGN KEY (profissional_id)
        REFERENCES profissionais (id),

    CONSTRAINT fk_profissionais_servicos_servico
        FOREIGN KEY (servico_id)
        REFERENCES servicos (id),

    CONSTRAINT uk_profissionais_servicos_profissional_servico
        UNIQUE (profissional_id, servico_id)
);
