-- Limpieza de lo importado de las planillas (ADR 0015).

-- 1) Características de uso interno: no salen en el sitio público. Se marca por ítem (editable en Catálogos).
ALTER TABLE sicat.caracteristica ADD COLUMN es_publica boolean NOT NULL DEFAULT true;

UPDATE sicat.caracteristica
SET es_publica = false
WHERE grupo IN ('Plataformas de venta', 'Redes que utiliza', 'Uso de redes sociales',
                'Información y calidad', 'Dominio', 'Otros', 'Seguridad');

CREATE OR REPLACE VIEW publico.v_caracteristica_publica AS
SELECT ec.id_establecimiento,
       ec.id_caracteristica,
       car.grupo,
       car.nombre,
       ec.cantidad
FROM sicat.establecimiento_caracteristica ec
JOIN publico.v_establecimiento_publico ep ON ep.id_establecimiento = ec.id_establecimiento
JOIN sicat.caracteristica car ON car.id_caracteristica = ec.id_caracteristica
WHERE car.activo
  AND car.es_publica;

-- 2) Notas internas del establecimiento (solo en el panel; ninguna vista pública las expone).
ALTER TABLE sicat.establecimiento ADD COLUMN notas_internas text;

-- 3) La importación dejó en la descripción las preguntas de la encuesta ("Pregunta: respuesta").
--    Esos párrafos pasan a notas_internas; el resto queda en la descripción.
--    El mismo criterio usa la importación (NotasInternas.java): mantenerlos iguales.
DO
$$
DECLARE
    e         record;
    parrafo   text;
    publicos  text[];
    internos  text[];
    patron    constant text :=
        '^\s*(s[ií] seleccion|seleccion|s[ií] marc[oó]|remodelaciones|reformas|cantidad de personal|capital humano'
        || '|organismo otorgante|distinciones? de calidad|estado de (las )?rampas|c?observaciones|mail)\s*[^:\n]{0,120}:';
BEGIN
    FOR e IN SELECT id_establecimiento, descripcion FROM sicat.establecimiento WHERE descripcion IS NOT NULL
    LOOP
        publicos := ARRAY[]::text[];
        internos := ARRAY[]::text[];
        FOREACH parrafo IN ARRAY regexp_split_to_array(e.descripcion, E'\n\n')
        LOOP
            IF parrafo ~* patron THEN
                internos := internos || parrafo;
            ELSE
                publicos := publicos || parrafo;
            END IF;
        END LOOP;
        IF cardinality(internos) > 0 THEN
            UPDATE sicat.establecimiento
            SET descripcion    = NULLIF(array_to_string(publicos, E'\n\n'), ''),
                notas_internas = array_to_string(internos, E'\n\n')
            WHERE id_establecimiento = e.id_establecimiento;
        END IF;
    END LOOP;
END;
$$;
