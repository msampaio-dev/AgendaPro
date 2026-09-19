INSERT INTO barbearias (nome) VALUES
    ('Barbershopping Ipanema'),
    ('Barbearia da Comunidade'),
    ('Barbershop Morumbi')
ON CONFLICT DO NOTHING;

INSERT INTO usuarios (nome, email, senha_hash, ativo) VALUES
    ('João Gabriel', 'joao.gabriel@demo.agendapro.local', '$2a$10$6aJYlQLXmmibEHQw4KwwSuxbADPAfQ.0eMznoAtiax7mTyy7JWJUC', TRUE),
    ('Victor Ruiz', 'victor.ruiz@demo.agendapro.local', '$2a$10$6aJYlQLXmmibEHQw4KwwSuxbADPAfQ.0eMznoAtiax7mTyy7JWJUC', TRUE),
    ('Henrique Silva', 'henrique.silva@demo.agendapro.local', '$2a$10$6aJYlQLXmmibEHQw4KwwSuxbADPAfQ.0eMznoAtiax7mTyy7JWJUC', TRUE),
    ('Junior Santos', 'junior.santos@demo.agendapro.local', '$2a$10$6aJYlQLXmmibEHQw4KwwSuxbADPAfQ.0eMznoAtiax7mTyy7JWJUC', TRUE),
    ('Rafael Oliveira', 'rafael.oliveira@demo.agendapro.local', '$2a$10$6aJYlQLXmmibEHQw4KwwSuxbADPAfQ.0eMznoAtiax7mTyy7JWJUC', TRUE),
    ('Wesley Gauchin', 'wesley.gauchin@demo.agendapro.local', '$2a$10$6aJYlQLXmmibEHQw4KwwSuxbADPAfQ.0eMznoAtiax7mTyy7JWJUC', TRUE)
ON CONFLICT DO NOTHING;

INSERT INTO usuarios_perfis (usuario_id, perfil)
SELECT id, perfil
FROM usuarios
CROSS JOIN (VALUES ('CLIENTE'), ('PROFISSIONAL')) AS perfis(perfil)
WHERE email LIKE '%@demo.agendapro.local'
ON CONFLICT DO NOTHING;

INSERT INTO profissionais (usuario_id, barbearia_id, ativo, fuso_horario)
SELECT u.id, b.id, TRUE, 'America/Sao_Paulo'
FROM usuarios u
JOIN barbearias b ON b.nome = CASE
    WHEN u.nome IN ('João Gabriel', 'Victor Ruiz') THEN 'Barbershopping Ipanema'
    WHEN u.nome = 'Henrique Silva' THEN 'Barbearia da Comunidade'
    ELSE 'Barbershop Morumbi'
END
WHERE u.email LIKE '%@demo.agendapro.local'
  AND NOT EXISTS (SELECT 1 FROM profissionais p WHERE p.usuario_id = u.id);

-- O catalogo pertence a barbearia, entao cada uma recebe os proprios servicos.
-- Os precos mudam de uma unidade para outra de proposito: e o que mostra na tela
-- que o catalogo deixou de ser global. A barba e a referencia fixa das tres.
--
-- ON CONFLICT atualiza em vez de ignorar porque esta migration e repetivel:
-- quando um preco muda aqui, o banco ja publicado precisa acompanhar na proxima
-- execucao. O indice unico e por barbearia e nome normalizado.
INSERT INTO servicos (barbearia_id, nome, descricao, duracao_minutos, preco, ativo)
SELECT b.id, catalogo.nome, catalogo.descricao, catalogo.duracao, catalogo.preco, TRUE
FROM barbearias b
JOIN (VALUES
    ('Barbershopping Ipanema', 'Corte de cabelo Social', 'Corte clássico com acabamento social.', 30, 50.00),
    ('Barbershopping Ipanema', 'Corte de cabelo Degradê', 'Corte com transição em degradê e acabamento.', 45, 60.00),
    ('Barbershopping Ipanema', 'Barba', 'Modelagem e acabamento completo da barba.', 30, 20.00),
    ('Barbearia da Comunidade', 'Corte de cabelo Social', 'Corte clássico com acabamento social.', 30, 35.00),
    ('Barbearia da Comunidade', 'Corte de cabelo Degradê', 'Corte com transição em degradê e acabamento.', 45, 42.00),
    ('Barbearia da Comunidade', 'Barba', 'Modelagem e acabamento completo da barba.', 30, 20.00),
    ('Barbershop Morumbi', 'Corte de cabelo Social', 'Corte clássico com acabamento social.', 30, 45.00),
    ('Barbershop Morumbi', 'Corte de cabelo Degradê', 'Corte com transição em degradê e acabamento.', 45, 55.00),
    ('Barbershop Morumbi', 'Barba', 'Modelagem e acabamento completo da barba.', 30, 20.00)
) AS catalogo(barbearia, nome, descricao, duracao, preco) ON catalogo.barbearia = b.nome
ON CONFLICT (barbearia_id, LOWER(BTRIM(nome))) DO UPDATE
SET descricao = EXCLUDED.descricao,
    duracao_minutos = EXCLUDED.duracao_minutos,
    preco = EXCLUDED.preco;

-- Cada profissional executa os servicos da propria barbearia.
INSERT INTO profissionais_servicos (profissional_id, servico_id, ativo)
SELECT p.id, s.id, TRUE
FROM profissionais p
JOIN usuarios u ON u.id = p.usuario_id AND u.email LIKE '%@demo.agendapro.local'
JOIN servicos s ON s.barbearia_id = p.barbearia_id
WHERE NOT EXISTS (
      SELECT 1 FROM profissionais_servicos ps
      WHERE ps.profissional_id = p.id AND ps.servico_id = s.id
  );

INSERT INTO horarios_atendimento (profissional_id, dia_semana, horario_inicio, horario_fim, ativo)
SELECT p.id, dias.dia, turnos.inicio, turnos.fim, TRUE
FROM profissionais p
JOIN usuarios u ON u.id = p.usuario_id AND u.email LIKE '%@demo.agendapro.local'
CROSS JOIN (VALUES ('MONDAY'), ('TUESDAY'), ('WEDNESDAY'), ('THURSDAY'), ('FRIDAY'), ('SATURDAY')) AS dias(dia)
CROSS JOIN (VALUES (TIME '09:00', TIME '12:00'), (TIME '13:00', TIME '19:00')) AS turnos(inicio, fim)
ON CONFLICT DO NOTHING;

UPDATE barbearias b
SET ativo = FALSE
WHERE b.nome = 'Unidade principal'
  AND NOT EXISTS (SELECT 1 FROM profissionais p WHERE p.barbearia_id = b.id);

UPDATE barbearias b
SET proprietario_profissional_id = p.id,
    cep = dados.cep,
    logradouro = dados.logradouro,
    numero = dados.numero,
    bairro = dados.bairro,
    cidade = 'São Paulo',
    estado = 'SP'
FROM profissionais p
JOIN usuarios u ON u.id = p.usuario_id
JOIN (VALUES
    ('Barbershopping Ipanema', 'joao.gabriel@demo.agendapro.local', '01415000', 'Rua Oscar Freire', '900', 'Jardins'),
    ('Barbearia da Comunidade', 'henrique.silva@demo.agendapro.local', '01310100', 'Avenida Paulista', '1000', 'Bela Vista'),
    ('Barbershop Morumbi', 'junior.santos@demo.agendapro.local', '05650000', 'Avenida Morumbi', '5000', 'Morumbi')
) AS dados(nome, email, cep, logradouro, numero, bairro)
    ON dados.email = u.email
WHERE b.nome = dados.nome;

INSERT INTO horarios_funcionamento_barbearia
    (barbearia_id, dia_semana, horario_inicio, horario_fim, ativo)
SELECT b.id, dias.dia, turnos.inicio, turnos.fim, TRUE
FROM barbearias b
CROSS JOIN (VALUES ('MONDAY'), ('TUESDAY'), ('WEDNESDAY'), ('THURSDAY'), ('FRIDAY'), ('SATURDAY')) AS dias(dia)
CROSS JOIN (VALUES (TIME '09:00', TIME '12:00'), (TIME '13:00', TIME '19:00')) AS turnos(inicio, fim)
WHERE b.nome IN ('Barbershopping Ipanema', 'Barbearia da Comunidade', 'Barbershop Morumbi')
ON CONFLICT DO NOTHING;
