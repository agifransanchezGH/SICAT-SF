# 0007 · Convenciones de los ABM de la API Admin

- Estado: Aceptado
- Fecha: 2026-11-12

## Contexto
El paso 5 agrega los ABM de catálogos, categorías, oferentes, lugares, establecimientos, ofertas y especializaciones. Hacían falta reglas comunes para bajas, listas, geografía y filtros que `ARQUITECTURA.md` no detallaba.

## Decisión

### Bajas
- **Lógicas** (`activo = false`) donde el modelo tiene `activo`: catálogos, categorías, oferentes, establecimientos, ofertas, salones. Los registros que las usan no cambian; las vistas públicas ya filtran por `activo`.
- Los listados omiten inactivos salvo `?incluirInactivos=true` (`incluirInactivas` en categorías y ofertas). Un `PUT` con `"activo": true` reactiva.
- **Físicas** donde no hay `activo`:
  - Lugar: solo si no contiene otros lugares ni lo usa un establecimiento o un circuito (si no, 409).
  - Visita guiada: se elimina con sus horarios. Para ocultarla sin borrarla, `habilitada = false`.
- Roles: las bajas de catálogos, categorías, oferentes, establecimientos y lugares son de ADMIN; las de ofertas, salones y visitas también de CARGADOR (son parte de la carga diaria).

### `PUT` que reemplazan listas
Contactos, referentes, horarios, distinciones, características, paradas y categorías adicionales se envían completas:
- con `id` (o con la clave natural: `distincionId`, `caracteristicaId`, orden de parada) → se modifica;
- sin `id` → se crea;
- lo que no viene → se elimina.

Se hace por diferencia y no "borrar todo e insertar", para que la auditoría registre solo lo que cambió. Un `id` que no pertenece al registro → 422.

### Geografía y filtros
- Coordenadas `{ lat, lng }`; geometrías y trazados en GeoJSON (`[lng, lat]`), con un conversor propio (`GeoJson` en `sicat-core`) para Point, LineString, Polygon y sus Multi.
- `bbox=minLng,minLat,maxLng,maxLat`, resuelto con PostGIS (`ST_Intersects`).
- El filtro `categoria` de establecimientos incluye la categoría, sus subcategorías y las categorías adicionales de ofertas activas.

### Reglas de negocio incluidas
- Árboles sin ciclos (categorías y lugares) → 422.
- La categoría principal de una oferta no puede repetirse como adicional (422); al cambiar la principal se quita de las adicionales.
- `cantidad` solo en características con `admiteCantidad` (422).
- Horarios: día 1–7; `desde` ≠ `hasta`; si `hasta` < `desde` la franja cruza la medianoche.
- Duplicados de catálogo (nombre sin distinguir mayúsculas; localidad por nombre + provincia; característica por grupo + nombre) → 409.
- Restricciones de la base no previstas → 409 genérico.

### No incluido en el paso 5
- No se valida que las características asignadas correspondan a las categorías del establecimiento (lo hará el front y, si hace falta, una regla posterior).
- Publicar / despublicar (paso 6), duplicados para la migración (paso 8), endpoints de mapa (paso 10).

## Consecuencias
- Los clientes deben mandar la lista completa en cada `PUT` de lista (no hay `PATCH` parcial).
- La auditoría queda limpia: un cambio de un horario genera un solo registro.
