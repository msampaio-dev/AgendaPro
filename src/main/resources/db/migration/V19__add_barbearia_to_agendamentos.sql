ALTER TABLE agendamentos ADD COLUMN barbearia_id BIGINT;

UPDATE agendamentos a
SET barbearia_id = p.barbearia_id
FROM profissionais p
WHERE p.id = a.profissional_id;

ALTER TABLE agendamentos ALTER COLUMN barbearia_id SET NOT NULL;

ALTER TABLE agendamentos
ADD CONSTRAINT fk_agendamentos_barbearia
FOREIGN KEY (barbearia_id) REFERENCES barbearias (id);

CREATE INDEX ix_agendamentos_barbearia_inicio
ON agendamentos (barbearia_id, inicio);
