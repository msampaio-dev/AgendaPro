CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE agendamentos
ADD CONSTRAINT ex_agendamentos_profissional_periodo
EXCLUDE USING gist (
    profissional_id WITH =,
    tstzrange(inicio, fim, '[)') WITH &&
)
WHERE (status IN ('AGENDADO', 'CONFIRMADO'));
