#!/bin/bash
# Se ejecuta una sola vez, al crear el volumen de datos (docker-entrypoint-initdb.d).
# Corre después de 10_postgis.sh de la imagen postgis/postgis.
# Crea los usuarios de aplicación. Los esquemas, tablas, vistas y GRANTs los crea Flyway
# (migraciones en backend/sicat-core/src/main/resources/db/migration).
set -euo pipefail

: "${SICAT_ADMIN_DB_USER:=sicat_admin_rw}"
: "${SICAT_PUBLIC_DB_USER:=sicat_public_ro}"
: "${SICAT_ADMIN_DB_PASSWORD:?Falta SICAT_ADMIN_DB_PASSWORD}"
: "${SICAT_PUBLIC_DB_PASSWORD:?Falta SICAT_PUBLIC_DB_PASSWORD}"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
  -v admin_user="$SICAT_ADMIN_DB_USER" -v admin_pass="$SICAT_ADMIN_DB_PASSWORD" \
  -v public_user="$SICAT_PUBLIC_DB_USER" -v public_pass="$SICAT_PUBLIC_DB_PASSWORD" \
  -v db="$POSTGRES_DB" <<'EOSQL'
CREATE EXTENSION IF NOT EXISTS postgis;

-- API Admin: dueño de los esquemas de la aplicación; ejecuta Flyway.
CREATE ROLE :"admin_user" LOGIN PASSWORD :'admin_pass';
GRANT CONNECT, CREATE, TEMPORARY ON DATABASE :"db" TO :"admin_user";

-- API Pública: solo lectura. Flyway le da USAGE sobre `publico` y SELECT sobre sus vistas.
CREATE ROLE :"public_user" LOGIN PASSWORD :'public_pass';
GRANT CONNECT ON DATABASE :"db" TO :"public_user";

-- Nadie crea objetos en el esquema public (donde vive PostGIS).
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
EOSQL
