-- SICAT SF · V5 · Usuarios y roles (esquema seguridad)
-- Fuente: docs/modelo-datos.md

CREATE TABLE seguridad.rol (
    id_rol      bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre      text NOT NULL UNIQUE,
    descripcion text
);

CREATE TABLE seguridad.usuario (
    id_usuario      bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_rol          bigint  NOT NULL REFERENCES seguridad.rol (id_rol),
    id_oferente     bigint  REFERENCES sicat.oferente (id_oferente),  -- reservado para la etapa 2
    nombre_completo text    NOT NULL,
    email           text    NOT NULL UNIQUE,
    hash_password   text    NOT NULL,
    activo          boolean NOT NULL DEFAULT true,
    ultimo_acceso   timestamptz
);
CREATE INDEX ix_usuario_rol      ON seguridad.usuario (id_rol);
CREATE INDEX ix_usuario_oferente ON seguridad.usuario (id_oferente);
