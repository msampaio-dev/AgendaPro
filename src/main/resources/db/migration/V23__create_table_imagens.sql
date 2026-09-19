-- As imagens deixam o disco e passam a viver no banco.
--
-- O sistema de arquivos do plano gratuito do Render e efemero: toda reimplantacao
-- recriava o container e levava junto as fotos enviadas, enquanto a coluna
-- foto_url continuava apontando para arquivos que nao existiam mais.
--
-- Guardar o binario aqui tambem faz as imagens acompanharem os branches do Neon,
-- que sao copias do banco: um branch passa a ter os dados e as fotos coerentes
-- entre si, em vez de dois mundos que podem divergir.
--
-- O nome ja e unico por construcao (prefixo + UUID + extensao) e e o que aparece
-- na URL, entao serve de chave primaria sem inventar um identificador novo.
CREATE TABLE imagens (
    nome VARCHAR(120) PRIMARY KEY,
    tipo_conteudo VARCHAR(60) NOT NULL,
    conteudo BYTEA NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL
);

-- As URLs guardadas hoje apontam para arquivos que ja nao existem: cada
-- reimplantacao recriou o container e levou o disco junto. Deixa-las intactas
-- exibiria imagem quebrada na tela; zeradas, volta o avatar padrao e quem
-- quiser reenvia a foto, que desta vez fica.
UPDATE barbearias SET foto_url = NULL WHERE foto_url IS NOT NULL;
UPDATE profissionais SET foto_url = NULL WHERE foto_url IS NOT NULL;
