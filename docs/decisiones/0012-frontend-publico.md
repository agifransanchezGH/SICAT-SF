# 0012 · Frontend del sitio público

- Estado: Aceptado
- Fecha: 2026-11-14

## Contexto
Segunda parte del paso 9: el sitio público (listado y ficha) sobre la API Pública (ADR 0009). Hacía falta definir la base visual y cómo probarlo, porque la base local no tenía nada publicado.

## Decisión
- **Misma base que el panel**: Angular Material (M3) con paleta neutra y variables `--sicat-*` en `projects/public/src/styles.scss`. La identidad visual oficial se aplica cambiando paletas y variables.
- **Listado** (`/`): buscador por texto (nombre y categorías, sin acentos, como la API), filtro por categoría (árbol aplanado) y por localidad (solo si hay más de una con publicados). Los filtros viven en la URL (`?q=…&categoria=…&localidad=…&pagina=…`), así se pueden compartir y "Volver" desde la ficha los conserva. Tarjetas con nombre, categorías, dirección y destacado; 24 por página.
- **Ficha** (`/establecimientos/:id`): nombre y categorías, aviso si no está abierto (estado de funcionamiento, motivo, reapertura), descripción, una tarjeta por oferta (acceso, público, frecuencia, horarios agrupados por día y el detalle de alojamiento, salones, visitas guiadas o circuito), características por grupo; al costado dirección con "Cómo llegar" (OpenStreetMap), contactos con enlace según el tipo (teléfono, correo, web, WhatsApp, redes) y distinciones. 404 → "Este lugar no está disponible".
- Los modelos de la librería `shared` ahora reflejan los DTO reales de la API Pública.
- Presupuesto del bundle inicial: 800 kB de aviso (hoy ~640 kB, ~150 kB transferidos).

## Consecuencias
- Para probar se publicaron 5 establecimientos reales cuya ubicación automática coincidía exactamente con OpenStreetMap: Convento San Francisco, Museo Etnográfico, Museo Rosa Galisteo, Hostal Santa Fe de la Vera Cruz y Catedral Metropolitana.
- Se ve en lo publicado que la importación dejó en la descripción respuestas de la encuesta y que hay grupos de características de uso interno (p. ej. "Plataformas de venta", "Uso de redes sociales") que hoy salen en la ficha. Queda para revisar.
- Paso 10: mapa en ambos sitios.
