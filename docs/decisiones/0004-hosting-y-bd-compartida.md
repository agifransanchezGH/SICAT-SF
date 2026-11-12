# 0004 · Hosting y base de datos compartida para el equipo

- Estado: **Propuesto** (pendiente de decisión del equipo)
- Fecha: 2026-11-12

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
Pendiente.

## Criterios para decidir
Costo, igualdad con el entorno local (mismas versiones de PostGIS), facilidad de dar acceso a nuevos integrantes, backups, y dónde deben residir los datos según la Municipalidad.
