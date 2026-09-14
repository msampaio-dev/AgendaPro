CREATE TABLE agendamentos (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    cliente_id BIGINT NOT NULL,
    profissional_id BIGINT NOT NULL,
    servico_id BIGINT NOT NULL,
    inicio TIMESTAMP WITH TIME ZONE NOT NULL,
    fim TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(12) NOT NULL DEFAULT 'AGENDADO',

    CONSTRAINT pk_agendamentos
        PRIMARY KEY (id),

    CONSTRAINT fk_agendamentos_cliente
        FOREIGN KEY (cliente_id)
        REFERENCES usuarios (id),

    CONSTRAINT fk_agendamentos_profissional
        FOREIGN KEY (profissional_id)
        REFERENCES profissionais (id),

    CONSTRAINT fk_agendamentos_servico
        FOREIGN KEY (servico_id)
        REFERENCES servicos (id),

    CONSTRAINT ck_agendamentos_intervalo_valido
        CHECK (inicio < fim),

    CONSTRAINT ck_agendamentos_status
        CHECK (status IN ('AGENDADO', 'CONFIRMADO', 'CANCELADO', 'CONCLUIDO'))
);

CREATE INDEX ix_agendamentos_profissional_inicio
ON agendamentos (profissional_id, inicio);

CREATE INDEX ix_agendamentos_cliente_inicio
ON agendamentos (cliente_id, inicio);
