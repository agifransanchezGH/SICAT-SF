# 0009 · API Pública: datos expuestos, búsqueda y caché

- Estado: Aceptado
- Fecha: 2026-11-14

## Contexto
El paso 7 implementa la API Pública sobre las vistas del esquema `publico`. La propuesta de vistas de `V8` quedó pendiente de revisión, y faltaba definir cómo buscar, cuánto cachear y qué devolver en la ficha.

## Decisión

### Datos públicos (revisados con el equipo)
Las vistas de `V8` quedan como estaban. Son públicos, además de los datos de la ficha:
- el nombre del oferente;
- estado de funcionamiento, motivo y fecha de reapertura (para avisar cierres temporales);
- los contactos públicos del establecimiento **y los de su oferente**, con su uso (p. ej. "Reservas"). Las redes sociales son canales de contacto (Instagram, Facebook, etc.) marcados como públicos;
- horarios de cada oferta y de cada visita guiada.

Siguen fuera del sitio: referentes, personal del alojamiento, estado de conservación y de funcionamiento de los salones, fecha de relevamiento, contactos no públicos, salones inactivos y visitas no habilitadas.

### Búsqueda (`?q=`)
- Busca en el **nombre y en las categorías** (principales y adicionales) de cada establecimiento, sin distinguir mayúsculas ni acentos. Cada palabra debe aparecer.
- `V9` agrega `publico.v_busqueda_publica` con ese texto ya normalizado (con `translate()`, sin depender de la extensión `unaccent`). La API normaliza el texto buscado igual.

### Filtros y geografía
- `categoria` incluye subcategorías y categorías adicionales; `bbox` filtra por lat/lng de la vista.
- `/cercanos` usa PostGIS sobre la vista (funciones de `public`, permitidas al usuario de solo lectura). Radio máximo 20 km, hasta 100 resultados.
- `/mapa` y `/mapa/circuitos` devuelven GeoJSON (`FeatureCollection`) listo para Leaflet.
- `/localidades` (agregado, no estaba en el diseño original): solo las que tienen establecimientos publicados, para el filtro del sitio.

### Caché y límites
- Caffeine en memoria, 5 minutos, por combinación de parámetros. Un establecimiento recién publicado o despublicado puede tardar hasta 5 minutos en reflejarse.
- 120 pedidos por minuto por IP (Bucket4j). Detrás de un proxy hay que configurar `server.forward-headers-strategy`.

### Zona horaria
Se quitó `hibernate.jdbc.time_zone: UTC` de ambas APIs: corría los campos `time` (horarios) según la zona de la JVM, así que un horario de 08:00 quedaba guardado como 11:00. Los `timestamptz` se guardan con su zona sin necesidad de esa opción. Hay un test que verifica la hora guardada en la base.

## Consecuencias
- Para hacer público un campo nuevo hay que agregarlo a la vista con una migración nueva (y al DTO de la ficha).
- Si la demora de 5 minutos molesta, se puede bajar el TTL o invalidar la caché desde la API Admin al publicar (requiere comunicación entre APIs; hoy no la hay a propósito).
