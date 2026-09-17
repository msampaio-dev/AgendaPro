CREATE TABLE horarios_funcionamento_barbearia (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    barbearia_id BIGINT NOT NULL,
    dia_semana VARCHAR(9) NOT NULL,
    horario_inicio TIME NOT NULL,
    horario_fim TIME NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_horarios_funcionamento_barbearia PRIMARY KEY (id),
    CONSTRAINT fk_horarios_funcionamento_barbearia
        FOREIGN KEY (barbearia_id) REFERENCES barbearias (id),
    CONSTRAINT ck_horarios_funcionamento_intervalo
        CHECK (horario_inicio < horario_fim),
    CONSTRAINT uk_horarios_funcionamento_intervalo
        UNIQUE (barbearia_id, dia_semana, horario_inicio, horario_fim)
);

CREATE INDEX ix_horarios_funcionamento_barbearia_dia
ON horarios_funcionamento_barbearia (barbearia_id, dia_semana)
WHERE ativo = TRUE;
