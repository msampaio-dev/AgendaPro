CREATE TABLE usuarios_perfis (
    usuario_id BIGINT NOT NULL,
    perfil VARCHAR(20) NOT NULL,

    CONSTRAINT pk_usuarios_perfis
        PRIMARY KEY (usuario_id, perfil),

    CONSTRAINT fk_usuarios_perfis_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuarios (id),

    CONSTRAINT ck_usuarios_perfis_perfil
        CHECK (perfil IN ('CLIENTE', 'PROFISSIONAL', 'ADMIN'))
);

INSERT INTO usuarios_perfis (usuario_id, perfil)
SELECT id, 'CLIENTE'
FROM usuarios;

INSERT INTO usuarios_perfis (usuario_id, perfil)
SELECT usuario_id, 'PROFISSIONAL'
FROM profissionais
WHERE ativo = TRUE;
