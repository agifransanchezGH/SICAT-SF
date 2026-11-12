-- SICAT SF · V1 · Esquemas
-- Flyway (API Admin, usuario sicat_admin_rw) crea los esquemas listados en spring.flyway.schemas.
-- Este script solo los documenta y asegura que existan.
-- Las extensiones (postgis) las crea el superusuario en infra/postgres/init/20_roles.sh.

CREATE SCHEMA IF NOT EXISTS sicat;
CREATE SCHEMA IF NOT EXISTS seguridad;
CREATE SCHEMA IF NOT EXISTS auditoria;
CREATE SCHEMA IF NOT EXISTS publico;

COMMENT ON SCHEMA sicat     IS 'Modelo de negocio de SICAT SF (ver docs/modelo-datos.md)';
COMMENT ON SCHEMA seguridad IS 'Usuarios y roles del panel de administración';
COMMENT ON SCHEMA auditoria IS 'Registro de cambios; separado de la lógica de negocio';
COMMENT ON SCHEMA publico   IS 'Solo vistas de lectura para la API Pública (usuario sicat_public_ro)';
