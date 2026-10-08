CREATE TABLE usos_sugestao_agendamento (
    usuario_id BIGINT NOT NULL,
    dia DATE NOT NULL,
    quantidade INTEGER NOT NULL,

    CONSTRAINT pk_usos_sugestao_agendamento PRIMARY KEY (usuario_id, dia),
    CONSTRAINT fk_usos_sugestao_agendamento_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT ck_usos_sugestao_agendamento_quantidade
        CHECK (quantidade > 0)
);

CREATE INDEX ix_usos_sugestao_agendamento_dia
ON usos_sugestao_agendamento (dia);
