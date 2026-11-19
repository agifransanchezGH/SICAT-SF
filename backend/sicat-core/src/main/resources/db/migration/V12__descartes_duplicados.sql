-- Grupos de "posibles duplicados" que una persona marcó como "no son duplicados" (p. ej. sucursales).
-- Un grupo son los establecimientos activos con el mismo nombre normalizado (clave). Se guarda la clave y la lista
-- ordenada de ids que tenía el grupo al descartarlo: si después entra o sale un integrante, la lista ya no coincide
-- y el grupo vuelve a aparecer para revisarlo. Quién y cuándo lo registra la auditoría (ADR 0006).

CREATE TABLE sicat.descarte_duplicado (
    id_descarte_duplicado bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    clave                 text NOT NULL,
    integrantes           text NOT NULL,
    CONSTRAINT uk_descarte_duplicado UNIQUE (clave, integrantes)
);

COMMENT ON COLUMN sicat.descarte_duplicado.clave IS 'Nombre normalizado del grupo (Texto.clave)';
COMMENT ON COLUMN sicat.descarte_duplicado.integrantes IS 'Ids de los establecimientos del grupo, ordenados y separados por coma';

-- Toda tabla de sicat lleva el aviso de cambios (ADR 0014).
CREATE TRIGGER avisar_cambio AFTER INSERT OR UPDATE OR DELETE OR TRUNCATE ON sicat.descarte_duplicado
    FOR EACH STATEMENT EXECUTE FUNCTION sicat.avisar_cambio();
