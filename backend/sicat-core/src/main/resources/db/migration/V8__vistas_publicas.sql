-- SICAT SF · V8 · Vistas públicas (PROPUESTA INICIAL, a revisar por el equipo)
-- Regla: un dato es público solo si está en una de estas vistas.
-- Todas parten de v_establecimiento_publico, que concentra el filtro de publicación:
--   establecimiento PUBLICADO y activo, oferente activo.
-- Las vistas pertenecen a sicat_admin_rw; sicat_public_ro solo tiene SELECT sobre ellas.
-- Coordenadas expuestas como lat/lng y trazados como GeoJSON (texto) para no exigir tipos PostGIS a la API Pública.

CREATE VIEW publico.v_establecimiento_publico AS
SELECT e.id_establecimiento,
       e.id_oferente,
       e.nombre,
       e.descripcion,
       e.anio_inauguracion,
       e.estado_funcionamiento,
       e.motivo_estado,
       e.fecha_reapertura,
       e.destacado,
       o.nombre                                   AS oferente,
       l.nombre                                   AS lugar,
       l.calle,
       l.altura,
       l.km,
       l.calle_cruce,
       l.complemento,
       loc.id_localidad,
       loc.nombre                                 AS localidad,
       loc.provincia,
       public.ST_Y(l.punto::public.geometry)      AS lat,
       public.ST_X(l.punto::public.geometry)      AS lng
FROM sicat.establecimiento e
JOIN sicat.oferente o       ON o.id_oferente = e.id_oferente
LEFT JOIN sicat.lugar l     ON l.id_lugar = e.id_lugar
LEFT JOIN sicat.localidad loc ON loc.id_localidad = l.id_localidad
WHERE e.estado_publicacion = 'PUBLICADO'
  AND e.activo
  AND o.activo;

CREATE VIEW publico.v_oferta_publica AS
SELECT ofe.id_oferta,
       ofe.id_establecimiento,
       ofe.id_categoria,
       c.nombre AS categoria,
       ofe.acceso,
       ofe.publico_admitido,
       ofe.frecuencia
FROM sicat.oferta ofe
JOIN publico.v_establecimiento_publico ep ON ep.id_establecimiento = ofe.id_establecimiento
JOIN sicat.categoria c ON c.id_categoria = ofe.id_categoria
WHERE ofe.activo
  AND c.activo;

CREATE VIEW publico.v_oferta_categoria_publica AS
SELECT oc.id_oferta,
       oc.id_categoria,
       c.nombre AS categoria
FROM sicat.oferta_categoria oc
JOIN publico.v_oferta_publica op ON op.id_oferta = oc.id_oferta
JOIN sicat.categoria c ON c.id_categoria = oc.id_categoria
WHERE c.activo;

CREATE VIEW publico.v_categoria_publica AS
SELECT id_categoria,
       id_categoria_padre,
       nombre
FROM sicat.categoria
WHERE activo;

-- Contactos públicos: los del establecimiento y los de su oferente (repetidos por establecimiento).
CREATE VIEW publico.v_contacto_publico AS
SELECT cc.id_canal,
       ep.id_establecimiento,
       tc.nombre AS tipo,
       cc.valor,
       cc.uso,
       cc.orden,
       'ESTABLECIMIENTO' AS origen
FROM sicat.canal_contacto cc
JOIN publico.v_establecimiento_publico ep ON ep.id_establecimiento = cc.id_establecimiento
JOIN sicat.tipo_canal tc ON tc.id_tipo_canal = cc.id_tipo_canal
WHERE cc.es_publico
  AND tc.activo
UNION ALL
SELECT cc.id_canal,
       ep.id_establecimiento,
       tc.nombre AS tipo,
       cc.valor,
       cc.uso,
       cc.orden,
       'OFERENTE' AS origen
FROM sicat.canal_contacto cc
JOIN publico.v_establecimiento_publico ep ON ep.id_oferente = cc.id_oferente
JOIN sicat.tipo_canal tc ON tc.id_tipo_canal = cc.id_tipo_canal
WHERE cc.es_publico
  AND tc.activo;

CREATE VIEW publico.v_caracteristica_publica AS
SELECT ec.id_establecimiento,
       ec.id_caracteristica,
       car.grupo,
       car.nombre,
       ec.cantidad
FROM sicat.establecimiento_caracteristica ec
JOIN publico.v_establecimiento_publico ep ON ep.id_establecimiento = ec.id_establecimiento
JOIN sicat.caracteristica car ON car.id_caracteristica = ec.id_caracteristica
WHERE car.activo;

CREATE VIEW publico.v_distincion_publica AS
SELECT ed.id_establecimiento,
       ed.id_distincion,
       d.nombre,
       d.organismo_otorgante,
       ed.anio_otorgamiento,
       ed.url_verificacion
FROM sicat.establecimiento_distincion ed
JOIN publico.v_establecimiento_publico ep ON ep.id_establecimiento = ed.id_establecimiento
JOIN sicat.distincion d ON d.id_distincion = ed.id_distincion
WHERE d.activo;

-- Especializaciones: sin datos internos (personal, estado de conservación).
CREATE VIEW publico.v_alojamiento_publico AS
SELECT a.id_oferta,
       a.estrellas,
       a.cant_habitaciones,
       a.total_plazas,
       a.hab_accesibles,
       a.plazas_accesibles
FROM sicat.alojamiento a
JOIN publico.v_oferta_publica op ON op.id_oferta = a.id_oferta;

CREATE VIEW publico.v_salon_publico AS
SELECT s.id_salon,
       s.id_oferta,
       s.nombre,
       s.cap_auditorio,
       s.cap_banquete,
       s.cap_coctel,
       s.m2
FROM sicat.salon s
JOIN publico.v_oferta_publica op ON op.id_oferta = s.id_oferta
WHERE s.activo;

CREATE VIEW publico.v_visita_guiada_publica AS
SELECT v.id_visita,
       v.id_oferta,
       v.modalidad,
       v.requiere_reserva
FROM sicat.visita_guiada v
JOIN publico.v_oferta_publica op ON op.id_oferta = v.id_oferta
WHERE v.habilitada;

CREATE VIEW publico.v_horario_publico AS
SELECT h.id_horario,
       h.id_oferta,
       h.id_visita,
       h.dia_semana,
       h.hora_desde,
       h.hora_hasta
FROM sicat.horario h
JOIN publico.v_oferta_publica op ON op.id_oferta = h.id_oferta
LEFT JOIN sicat.visita_guiada v ON v.id_visita = h.id_visita
WHERE h.id_visita IS NULL
   OR v.habilitada;

CREATE VIEW publico.v_circuito_publico AS
SELECT ci.id_oferta,
       ci.duracion_min,
       lp.nombre                                  AS lugar_partida,
       public.ST_Y(lp.punto::public.geometry)     AS lat_partida,
       public.ST_X(lp.punto::public.geometry)     AS lng_partida,
       public.ST_AsGeoJSON(ci.trazado)            AS trazado_geojson
FROM sicat.circuito ci
JOIN publico.v_oferta_publica op ON op.id_oferta = ci.id_oferta
LEFT JOIN sicat.lugar lp ON lp.id_lugar = ci.id_lugar_partida;

-- Solo paradas en establecimientos publicados.
CREATE VIEW publico.v_circuito_parada_publica AS
SELECT cp.id_oferta,
       cp.orden,
       cp.id_establecimiento,
       ep.nombre,
       ep.lat,
       ep.lng
FROM sicat.circuito_parada cp
JOIN publico.v_circuito_publico ci ON ci.id_oferta = cp.id_oferta
JOIN publico.v_establecimiento_publico ep ON ep.id_establecimiento = cp.id_establecimiento;

-- Permisos del usuario de solo lectura ---------------------------------------
GRANT USAGE ON SCHEMA publico TO ${publicUser};
GRANT SELECT ON ALL TABLES IN SCHEMA publico TO ${publicUser};
ALTER DEFAULT PRIVILEGES IN SCHEMA publico GRANT SELECT ON TABLES TO ${publicUser};
