-- SICAT SF · V6 · Auditoría (esquema auditoria)
-- Fuente: docs/modelo-datos.md

CREATE TABLE auditoria.auditoria (
    id_auditoria     bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tabla_afectada   text        NOT NULL,
    clave_registro   text        NOT NULL,
    accion           text        NOT NULL,
    id_usuario       bigint      REFERENCES seguridad.usuario (id_usuario),
    fecha_hora       timestamptz NOT NULL DEFAULT now(),
    origen           text        NOT NULL DEFAULT 'APP',
    datos_anteriores jsonb,
    datos_nuevos     jsonb,
    CONSTRAINT ck_auditoria_accion CHECK (accion IN ('ALTA', 'MODIFICACION', 'ELIMINACION')),
    CONSTRAINT ck_auditoria_origen CHECK (origen IN ('APP', 'MIGRACION', 'SISTEMA'))
);
CREATE INDEX ix_auditoria_registro   ON auditoria.auditoria (tabla_afectada, clave_registro);
CREATE INDEX ix_auditoria_usuario    ON auditoria.auditoria (id_usuario);
CREATE INDEX ix_auditoria_fecha_hora ON auditoria.auditoria (fecha_hora);
