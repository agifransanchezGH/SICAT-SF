-- SICAT SF · V9 · Vista de búsqueda para la API Pública
-- Texto normalizado (minúsculas, sin acentos) con el nombre del establecimiento y los nombres de las
-- categorías de sus ofertas públicas (principal y adicionales). La API Pública busca ?q= sobre este texto.
-- Se normaliza con translate() para no depender de la extensión unaccent; la API aplica la misma
-- normalización al texto buscado.

CREATE VIEW publico.v_busqueda_publica AS
SELECT ep.id_establecimiento,
       lower(translate(
           concat_ws(' ', ep.nombre, string_agg(DISTINCT op.categoria, ' '), string_agg(DISTINCT oc.categoria, ' ')),
           'ÁÉÍÓÚÜÑáéíóúüñ',
           'AEIOUUNaeiouun')) AS texto
FROM publico.v_establecimiento_publico ep
LEFT JOIN publico.v_oferta_publica op ON op.id_establecimiento = ep.id_establecimiento
LEFT JOIN publico.v_oferta_categoria_publica oc ON oc.id_oferta = op.id_oferta
GROUP BY ep.id_establecimiento, ep.nombre;

GRANT SELECT ON publico.v_busqueda_publica TO ${publicUser};
