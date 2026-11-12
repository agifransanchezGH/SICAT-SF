# 0002 · Publicación por vistas y usuario de solo lectura

- Estado: Aceptado
- Fecha: 2026-11-12

## Contexto
Hay que garantizar que el sitio público muestre solo lo publicado y solo los campos aptos para el público, aunque haya errores en el código.

## Decisión
- Los datos públicos se definen en vistas del esquema `publico` (`V8__vistas_publicas.sql` y siguientes).
- Las vistas filtran `estado_publicacion = 'PUBLICADO'`, registros `activo` y contactos con `es_publico = true`.
- La API Pública se conecta con `sicat_public_ro`, que solo tiene `USAGE` sobre `publico` y `SELECT` sobre sus vistas. No tiene permisos sobre `sicat`, `seguridad` ni `auditoria`.
- Las vistas pertenecen a `sicat_admin_rw` (owner), así pueden leer `sicat` sin dar permisos al usuario público.
- Flyway está deshabilitado en la API Pública.

## Consecuencias
- Un campo nuevo es público solo si se agrega a una vista con una migración nueva.
- Si una columna no está en la vista, la API Pública no puede filtrarla ni devolverla.
- Cambios en la forma pública requieren migración y despliegue de la API Admin primero.
