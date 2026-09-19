-- Servico deixa de ser catalogo global da plataforma e passa a pertencer a uma
-- barbearia, para que cada uma defina seus proprios servicos e precos.
--
-- O backfill precisa de cuidado: no modelo antigo um mesmo servico podia ser
-- executado por profissionais de barbearias diferentes. Simplesmente escolher
-- uma dona roubaria o servico das demais, entao cada barbearia que usava um
-- servico de outra recebe a propria copia, e as referencias sao reapontadas.

ALTER TABLE servicos ADD COLUMN barbearia_id BIGINT;

-- Coluna temporaria: liga cada copia ao servico que a originou, para reapontar
-- profissionais_servicos e agendamentos logo abaixo.
ALTER TABLE servicos ADD COLUMN origem_migracao_id BIGINT;

-- A dona de cada servico passa a ser a primeira barbearia que o utilizava.
UPDATE servicos s
SET barbearia_id = origem.barbearia_id
FROM (
    SELECT ps.servico_id, MIN(p.barbearia_id) AS barbearia_id
    FROM profissionais_servicos ps
    JOIN profissionais p ON p.id = ps.profissional_id
    GROUP BY ps.servico_id
) origem
WHERE s.id = origem.servico_id;

-- Servicos que ninguem executa ficam com a barbearia mais antiga: sem dona a
-- coluna nao pode virar NOT NULL, e apagar perderia historico de agendamentos.
UPDATE servicos
SET barbearia_id = (SELECT MIN(id) FROM barbearias)
WHERE barbearia_id IS NULL;

-- Uma copia por barbearia que usava um servico que agora pertence a outra.
INSERT INTO servicos (
    nome, descricao, duracao_minutos, preco, ativo, barbearia_id, origem_migracao_id
)
SELECT s.nome, s.descricao, s.duracao_minutos, s.preco, s.ativo, uso.barbearia_id, s.id
FROM (
    SELECT DISTINCT p.barbearia_id, ps.servico_id
    FROM profissionais_servicos ps
    JOIN profissionais p ON p.id = ps.profissional_id
) uso
JOIN servicos s ON s.id = uso.servico_id
WHERE s.barbearia_id <> uso.barbearia_id;

-- Cada associacao passa a apontar para o servico da barbearia do profissional.
UPDATE profissionais_servicos ps
SET servico_id = copia.id
FROM profissionais p, servicos copia
WHERE p.id = ps.profissional_id
  AND copia.origem_migracao_id = ps.servico_id
  AND copia.barbearia_id = p.barbearia_id;

-- O mesmo para o historico de agendamentos, que guarda a barbearia atendida.
UPDATE agendamentos a
SET servico_id = copia.id
FROM servicos copia
WHERE copia.origem_migracao_id = a.servico_id
  AND copia.barbearia_id = a.barbearia_id;

UPDATE agendamentos a
SET servico_adicional_id = copia.id
FROM servicos copia
WHERE a.servico_adicional_id IS NOT NULL
  AND copia.origem_migracao_id = a.servico_adicional_id
  AND copia.barbearia_id = a.barbearia_id;

ALTER TABLE servicos DROP COLUMN origem_migracao_id;

ALTER TABLE servicos ALTER COLUMN barbearia_id SET NOT NULL;

ALTER TABLE servicos
ADD CONSTRAINT fk_servicos_barbearia
FOREIGN KEY (barbearia_id) REFERENCES barbearias (id);

CREATE INDEX ix_servicos_barbearia ON servicos (barbearia_id);

-- O nome deixa de ser unico na plataforma e passa a ser unico dentro da
-- barbearia: duas barbearias podem oferecer "Barba" com precos diferentes.
DROP INDEX uk_servicos_nome_normalizado;

CREATE UNIQUE INDEX uk_servicos_barbearia_nome_normalizado
ON servicos (barbearia_id, LOWER(BTRIM(nome)));
