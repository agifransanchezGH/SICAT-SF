-- Aviso de cambios para la caché de la API Pública (ADR 0014).
-- Cada INSERT/UPDATE/DELETE en una tabla de sicat hace pg_notify('sicat_cambios', '<tabla>').
-- PostgreSQL entrega el aviso recién al confirmar la transacción y junta los repetidos
-- (mismo canal y mismo texto) dentro de una transacción: una importación grande manda un aviso por tabla.
-- La API Pública escucha el canal (LISTEN) y vacía su caché.

CREATE OR REPLACE FUNCTION sicat.avisar_cambio() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    PERFORM pg_notify('sicat_cambios', TG_TABLE_NAME);
    RETURN NULL;
END;
$$;

-- Un trigger por sentencia (no por fila) en cada tabla del esquema de negocio.
-- Una tabla nueva de sicat necesita su trigger en su propia migración (ver ADR 0014).
DO
$$
DECLARE
    t record;
BEGIN
    FOR t IN
        SELECT table_name
        FROM information_schema.tables
        WHERE table_schema = 'sicat' AND table_type = 'BASE TABLE'
          -- La tabla de Flyway vive en sicat: un trigger ahí bloquea a Flyway mientras registra esta migración.
          AND table_name <> 'flyway_schema_history'
    LOOP
        EXECUTE format(
            'CREATE TRIGGER avisar_cambio AFTER INSERT OR UPDATE OR DELETE OR TRUNCATE ON sicat.%I '
                || 'FOR EACH STATEMENT EXECUTE FUNCTION sicat.avisar_cambio()',
            t.table_name);
    END LOOP;
END;
$$;
