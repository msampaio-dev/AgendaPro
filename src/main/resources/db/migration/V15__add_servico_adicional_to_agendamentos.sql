ALTER TABLE agendamentos ADD COLUMN servico_adicional_id BIGINT;

ALTER TABLE agendamentos
ADD CONSTRAINT fk_agendamentos_servico_adicional
FOREIGN KEY (servico_adicional_id) REFERENCES servicos (id);

ALTER TABLE agendamentos
ADD CONSTRAINT ck_agendamentos_servicos_distintos
CHECK (servico_adicional_id IS NULL OR servico_adicional_id <> servico_id);
