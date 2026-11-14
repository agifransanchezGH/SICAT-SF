# SICAT SF

Sistema de información turística de la Municipalidad de Santa Fe. Backend Java + Spring Boot, frontend Angular, base PostgreSQL + PostGIS.

## Antes de cualquier tarea

1. Leer `ARQUITECTURA.md` (arquitectura, estructura de carpetas, endpoints y orden de construcción).
2. Leer `docs/modelo-datos.md` antes de crear o modificar tablas o entidades. Es la fuente de verdad del modelo: si otro documento difiere, manda `modelo-datos.md`. (`docs/SICAT_SF_DER.drawio` es el diagrama.)

## Reglas que no se rompen

- La API pública (`sicat-public-api`) solo lee el esquema `publico` (vistas) con el usuario de solo lectura. Nunca depende de `sicat-admin-api`.
- Solo la API Admin ejecuta migraciones Flyway. Todo cambio de esquema es una migración nueva (`V<n>__descripcion.sql`); nunca se edita una ya aplicada.
- No exponer entidades JPA por HTTP: siempre DTOs.
- La auditoría vive en su propio paquete, separada de la lógica de negocio. Es automática para todo cambio hecho por JPA; no agregar código de auditoría en los servicios y evitar SQL directo para escribir datos de negocio (no se auditaría).
- Un campo es público solo si se agrega explícitamente a la vista correspondiente.
- Cada endpoint nuevo lleva DTO, validaciones, documentación OpenAPI y al menos un test.

## Convenciones

- Dominio y nombres de negocio en español; infraestructura en inglés técnico estándar.
- Rutas: `/api/admin/v1` y `/api/public/v1`. Errores en formato RFC 7807.
- Ante una duda de diseño no cubierta en `ARQUITECTURA.md`, preguntar antes de decidir.

## Comandos

- Levantar entorno completo: `docker compose up -d --build` (requiere `.env`, copiar de `.env.example`)
- Solo base de datos: `docker compose up -d db`
- Backend (tests): `cd backend && ./mvnw verify` (los tests de integración usan Testcontainers: requieren Docker)
- Correr una API: `./mvnw -pl sicat-admin-api spring-boot:run` / `./mvnw -pl sicat-public-api spring-boot:run`
- Frontend: `cd frontend && npm install && npx ng serve admin` / `npx ng serve public`
- Build frontend: `npx ng build admin` / `npx ng build public`
