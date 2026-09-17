CREATE UNIQUE INDEX uk_barbearias_proprietario_ativo
ON barbearias (proprietario_profissional_id)
WHERE ativo = TRUE AND proprietario_profissional_id IS NOT NULL;
