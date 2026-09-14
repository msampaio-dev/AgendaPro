CREATE TABLE excecoes_disponibilidade (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    profissional_id BIGINT NOT NULL,
    data DATE NOT NULL,
    tipo VARCHAR(24) NOT NULL,
    horario_inicio TIME,
    horario_fim TIME,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_excecoes_disponibilidade
        PRIMARY KEY (id),

    CONSTRAINT fk_excecoes_disponibilidade_profissional
        FOREIGN KEY (profissional_id)
        REFERENCES profissionais (id),

    CONSTRAINT ck_excecoes_disponibilidade_tipo
        CHECK (tipo IN ('BLOQUEIO', 'DISPONIBILIDADE_EXTRA')),

    CONSTRAINT ck_excecoes_disponibilidade_intervalo
        CHECK (
            (horario_inicio IS NULL AND horario_fim IS NULL)
            OR
            (
                horario_inicio IS NOT NULL
                AND horario_fim IS NOT NULL
                AND horario_inicio < horario_fim
            )
        ),

    CONSTRAINT ck_excecoes_disponibilidade_extra_com_intervalo
        CHECK (
            tipo <> 'DISPONIBILIDADE_EXTRA'
            OR
            (horario_inicio IS NOT NULL AND horario_fim IS NOT NULL)
        )
);
