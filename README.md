# SICAT SF

Sistema de información turística de la Municipalidad de Santa Fe.

- Arquitectura, estructura y endpoints: [ARQUITECTURA.md](ARQUITECTURA.md)
- Modelo de datos (fuente de verdad): [docs/modelo-datos.md](docs/modelo-datos.md)
- Decisiones: [docs/decisiones/](docs/decisiones/)

## Requisitos

- Docker Desktop (alcanza para levantar todo)
- Para desarrollar sin Docker: Java 21+, Node 22+ (Maven no hace falta: se usa `./mvnw`)

## Levantar todo con Docker

```bash
cp .env.example .env        # y cambiar contraseñas
docker compose up -d --build
```

| Servicio | URL |
|---|---|
| API Admin + Swagger | http://localhost:8081/swagger-ui.html |
| API Pública + Swagger | http://localhost:8082/swagger-ui.html |
| Front admin | http://localhost:4200 |
| Front público | http://localhost:4300 |
| PostgreSQL | localhost:5432 (base `sicat`) |

La API Admin ejecuta las migraciones Flyway al arrancar; la API Pública arranca después y solo lee el esquema `publico`.

## Desarrollo local

Base de datos sola:

```bash
docker compose up -d db
```

Backend:

```bash
cd backend
./mvnw verify                                   # compila y corre tests (Testcontainers necesita Docker)
./mvnw -pl sicat-admin-api spring-boot:run      # API Admin en :8081
./mvnw -pl sicat-public-api spring-boot:run     # API Pública en :8082
```

Frontend:

```bash
cd frontend
npm install
npx ng serve admin      # :4200, proxy /api -> :8081
npx ng serve public     # :4300, proxy /api -> :8082
```

## Estructura

```
backend/    Maven multi-módulo: sicat-core, sicat-admin-api, sicat-public-api
frontend/   Workspace Angular: projects/admin, projects/public, projects/shared
infra/      Inicialización de PostgreSQL (roles) y nginx opcional
docs/       Modelo de datos, diagramas y ADRs
```
