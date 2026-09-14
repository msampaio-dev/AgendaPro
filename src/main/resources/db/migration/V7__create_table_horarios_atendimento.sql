CREATE TABLE horarios_atendimento (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    profissional_id BIGINT NOT NULL,
    dia_semana VARCHAR(9) NOT NULL,
    horario_inicio TIME NOT NULL,
    horario_fim TIME NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_horarios_atendimento
        PRIMARY KEY (id),

    CONSTRAINT fk_horarios_atendimento_profissional
        FOREIGN KEY (profissional_id)
        REFERENCES profissionais (id),

    CONSTRAINT ck_horarios_atendimento_dia_semana
        CHECK (dia_semana IN (
            'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY',
            'FRIDAY', 'SATURDAY', 'SUNDAY'
        )),

    CONSTRAINT ck_horarios_atendimento_intervalo_valido
        CHECK (horario_inicio < horario_fim),

    CONSTRAINT uk_horarios_atendimento_intervalo
        UNIQUE (profissional_id, dia_semana, horario_inicio, horario_fim)
);
