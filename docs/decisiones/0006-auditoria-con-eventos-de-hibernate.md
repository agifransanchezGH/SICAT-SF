# 0006 · Auditoría con eventos de Hibernate

- Estado: Aceptado
- Fecha: 2026-11-12

## Contexto
Todo cambio hecho desde el panel (y la importación desde Excel) tiene que quedar registrado en `auditoria.auditoria`, antes de construir los ABM. La regla 5 de `ARQUITECTURA.md` exige que la auditoría esté separada de la lógica de negocio: ni las entidades ni los servicios de dominio la referencian.

Opciones consideradas:
- `@EntityListeners` en cada entidad: obliga a que el dominio conozca la auditoría (viola la regla 5).
- Triggers en PostgreSQL: capturan todo, pero el usuario del JWT hay que pasarlo con variables de sesión y la lógica queda en SQL.
- **Listeners de eventos de Hibernate** registrados desde el paquete `auditoria`: genéricos, sin tocar el dominio.

## Decisión
- `AuditoriaListener` (paquete `ar.sf.sicat.admin.auditoria`) escucha `POST_INSERT`, `POST_UPDATE`, `POST_DELETE`, `POST_COLLECTION_UPDATE` y `POST_COLLECTION_RECREATE`. Lo registra `AuditoriaIntegrador` (un `HibernatePropertiesCustomizer`).
- Escribe con JDBC sobre la misma conexión, **en la misma transacción** que el cambio: si se revierte, la auditoría también.
- Qué guarda:
  - `tabla_afectada`: `esquema.tabla` (p. ej. `sicat.establecimiento`).
  - `clave_registro`: el id; si la clave es compuesta, JSON (`{"idEstablecimiento":1,"idDistincion":2}`).
  - ALTA: `datos_nuevos` con todos los campos. MODIFICACION: solo los campos que cambiaron, antes y después. ELIMINACION: `datos_anteriores` con todos los campos.
  - Asociaciones como el id referido; geometrías como WKT; colecciones `@ManyToMany` como MODIFICACION del dueño con la lista de ids.
  - Nombres de campos de Java (camelCase), como en la API.
- `id_usuario`: el `sub` del JWT del pedido. `origen`: `MIGRACION` si el pedido es a `/importacion/**`, `APP` si hay usuario, `SISTEMA` si no (p. ej. el ADMIN inicial).
- `hashPassword` nunca se guarda (se anota `"(modificada)"`). `ultimoAcceso` se ignora para que el login no genere registros.
- Consulta: `GET /auditoria` (ADMIN, con filtros y paginado) y `GET /auditoria/{tabla}/{clave}` (ADMIN, CARGADOR).

## Consecuencias
- Los ABM nuevos quedan auditados sin escribir código de auditoría, siempre que modifiquen entidades por JPA.
- **No se audita** lo que se haga con SQL directo, `@Modifying` JPQL masivos o JDBC: si la importación usa inserciones masivas, deberá pasar por entidades o registrar su propia auditoría.
- Reemplazar una colección completa (`setX(nuevoSet)`) registra solo el estado nuevo; modificarla en el lugar (`getX().add(...)`) registra antes y después.
- Al crear una entidad con una colección ya cargada se generan dos registros: el ALTA y una MODIFICACION con la colección.
