# 0004 · Hosting y base de datos compartida para el equipo

- Estado: **Parcialmente decidido** (demo y proveedor de teselas definidos; hosting de producción pendiente del equipo)
- Fecha: 2026-11-12
- Actualizado: 2026-11-22 (demo y teselas)

## Contexto
Todo corre en contenedores Docker. Falta definir dónde alojar las APIs y la base, y cómo compartir una base de datos con la que todos los miembros puedan probar en tiempo real. Requisitos: PostgreSQL 17 + PostGIS, costo bajo o nulo en etapa de desarrollo, acceso seguro (no exponer el puerto 5432 abierto a internet), y que la zona pública quede aislada de la administración.

## Opciones a evaluar

### Base de datos compartida (desarrollo / pruebas)
| Opción | PostGIS | Pros | Contras |
|---|---|---|---|
| Supabase (plan gratuito) | Sí | Postgres gestionado, panel web, backups | Límite de tamaño y pausa por inactividad; roles propios a adaptar |
| Neon (plan gratuito) | Sí | Ramas de base por desarrollador, serverless | Arranque en frío; límites de cómputo |
| VPS propio con Docker (p. ej. Oracle Cloud Free, Hetzner) | Sí (misma imagen que local) | Igual al entorno local, control total | Mantenimiento, backups y seguridad a cargo del equipo |
| Servidor de la Municipalidad | Sí | Datos dentro de la institución | Depende de infraestructura y permisos municipales |

Acceso recomendado en cualquier caso: red privada tipo Tailscale/WireGuard o túnel SSH, en lugar de abrir el puerto públicamente.

### Hosting de las aplicaciones
- Contenedores en VPS con `docker compose` + nginx (reverse proxy y HTTPS con Let's Encrypt).
- PaaS con soporte de contenedores (Render, Railway, Fly.io) para APIs y front.
- Front público como sitio estático (CDN) apuntando a la API Pública; opción de instantánea GeoJSON estática (nivel 2 del diagrama).

## Decisión

### Demo (hoy)
- La demo corre en la PC de un integrante con `docker compose`; el equipo accede por una red privada Tailscale, sin abrir puertos a internet. No tiene costo y los datos no salen de esa PC; depende de que esa PC esté encendida.
- El mapa usa las teselas del servidor público de OpenStreetMap, solo para demo y poco tráfico (ADR 0013).

### Teselas del mapa en producción
- Se prevé **MapTiler** como proveedor de teselas. Leaflet se mantiene como librería (las teselas son solo la imagen de fondo): cambia la URL de la capa en `shared/src/lib/mapa/mapa.ts` y se suma una clave de API restringida por dominio. Se confirma al pasar a producción.
- Alternativas consideradas:
  - OpenStreetMap público: descartado para producción; su política prohíbe el uso intensivo y puede bloquear el acceso sin aviso.
  - Stadia Maps: plan gratuito solo no comercial (200.000 créditos por mes); los planes pagos empiezan en USD 20 por mes.
  - Teselas propias con Protomaps/PMTiles servidas desde el mismo servidor: sin cuotas ni claves, pero exige pasar a teselas vectoriales (más cambios de código).
- A verificar con MapTiler antes de producción: el plan gratuito es solo no comercial, muestra su logo en el mapa y se detiene al agotar la cuota (5.000 sesiones de mapa y 100.000 solicitudes por mes). Hay que confirmar si un sitio institucional de la Municipalidad entra como no comercial o si hace falta un plan pago, y revisar los precios vigentes.

### Pendiente
- Dónde alojar las APIs y la base en producción (VPS, servidor municipal u otra opción de las de arriba) y la base compartida de pruebas. Depende de lo que defina la Municipalidad sobre dónde pueden residir los datos.

## Criterios para decidir
Costo, igualdad con el entorno local (mismas versiones de PostGIS), facilidad de dar acceso a nuevos integrantes, backups, y dónde deben residir los datos según la Municipalidad.
