CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE usuarios
    ADD COLUMN senha_hash VARCHAR(100);

UPDATE usuarios
SET senha_hash = crypt(gen_random_uuid()::text, gen_salt('bf', 12));

ALTER TABLE usuarios
    ALTER COLUMN senha_hash SET NOT NULL;
