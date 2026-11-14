# 0014 · Caché pública que se vacía al instante (LISTEN/NOTIFY)

- Estado: Aceptado
- Fecha: 2026-11-14

## Contexto
La API Pública cachea sus respuestas 5 minutos (ADR 0009). Al probar el panel, un cambio en un contacto (marcar Instagram como público) no se veía en el sitio aunque se recargara: la ficha había quedado en caché con el dato anterior. Para quien carga datos parece una falla.

## Decisión
- **La base avisa**: la migración `V10` crea `sicat.avisar_cambio()` y un trigger *por sentencia* (no por fila) en cada tabla de `sicat`, que hace `pg_notify('sicat_cambios', <tabla>)`. Se excluye `flyway_schema_history`, que vive en el mismo esquema (un trigger ahí traba a Flyway mientras registra la migración).
- PostgreSQL entrega el aviso **al confirmar** la transacción (si se revierte, no avisa) y junta los repetidos de una misma transacción: una importación grande genera un aviso por tabla, no miles.
- **La API Pública escucha**: `AvisoDeCambios` abre una conexión propia (fuera del pool) con `sicat_public_ro`, hace `LISTEN sicat_cambios` y, ante cualquier aviso, vacía toda la caché. Si la conexión se corta, reintenta cada 5 s y vacía la caché al reconectar, porque pudo perderse un aviso.
- Se mantiene el vencimiento de 5 minutos como respaldo.
- No hay comunicación entre las APIs: sigue sin existir dependencia del módulo público hacia el admin, y también cubre cambios hechos fuera del panel (importación, SQL).

## Consecuencias
- Un cambio guardado en el panel se ve en el sitio en menos de un segundo.
- Cada cambio vacía toda la caché (no solo la ficha afectada). Con el volumen de cambios del panel es despreciable; si algún día hay escrituras masivas y continuas, se puede invalidar por tabla usando el texto del aviso.
- **Una tabla nueva en `sicat` necesita su trigger** en la misma migración que la crea:
  `CREATE TRIGGER avisar_cambio AFTER INSERT OR UPDATE OR DELETE OR TRUNCATE ON sicat.<tabla> FOR EACH STATEMENT EXECUTE FUNCTION sicat.avisar_cambio();`
- Con varias instancias de la API Pública, cada una escucha y vacía la suya.
