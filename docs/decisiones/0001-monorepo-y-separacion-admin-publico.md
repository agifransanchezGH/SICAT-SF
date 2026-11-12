# 0001 · Monorepo y separación admin / público

- Estado: Aceptado
- Fecha: 2026-11-12

## Contexto
La parte pública (ciudadanos, muchos usuarios concurrentes) no puede afectar a la administración (personal municipal). El equipo es chico y comparte backend, frontend e infraestructura.

## Decisión
- Un único repositorio con `backend/`, `frontend/`, `infra/` y `docs/`.
- Dos APIs Spring Boot desplegables por separado: `sicat-admin-api` (lectura/escritura, interna) y `sicat-public-api` (solo lectura, expuesta). Lo compartido vive en `sicat-core`.
- `sicat-public-api` nunca depende de `sicat-admin-api`. Se verifica con un test ArchUnit.
- Un workspace Angular con dos aplicaciones (`admin`, `public`) y una librería `shared`.

## Consecuencias
- Se puede escalar, cachear o apagar la parte pública sin tocar la administración.
- Un solo `docker compose` levanta todo el entorno.
- Hay que cuidar que `sicat-core` no acumule lógica propia de una sola API.
