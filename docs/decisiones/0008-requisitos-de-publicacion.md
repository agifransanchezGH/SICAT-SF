# 0008 · Requisitos para publicar un establecimiento

- Estado: Aceptado
- Fecha: 2026-11-13

## Contexto
`ARQUITECTURA.md` pedía que `/publicar` validara "datos mínimos" sin definirlos. Además había que decidir qué pasa cuando un establecimiento ya publicado se modifica o se da de baja.

## Decisión

### Requisitos para pasar a PUBLICADO
1. El establecimiento está activo.
2. Su oferente está activo.
3. Tiene un lugar asignado **con coordenadas** (para que aparezca en el mapa).
4. Tiene **al menos una oferta activa** cuya categoría principal está activa.

La descripción y los contactos públicos no son obligatorios.

`GET /establecimientos/{id}/publicacion` devuelve el estado y la lista de requisitos que faltan (para un checklist en el front). `POST /publicar` responde 422 con esa lista en la propiedad `errores` del Problem Details.

### Estados
- `BORRADOR` → `PUBLICADO` con `/publicar`.
- `PUBLICADO` → `OCULTO` con `/despublicar`; `OCULTO` → `PUBLICADO` con `/publicar`.
- `/despublicar` sobre un `BORRADOR` → 422 (nunca se publicó).
- `/publicar` sobre uno ya publicado vuelve a validar y no cambia nada.

### Un publicado nunca queda incompleto
Si un cambio deja a un establecimiento `PUBLICADO` sin los requisitos, **se rechaza con 422** y no se guarda. Hay que despublicarlo primero. Se controla en:
- modificar el establecimiento (p. ej. quitarle el lugar);
- modificar el lugar (p. ej. quitarle las coordenadas);
- dar de baja o desactivar una oferta (si era la única válida);
- dar de baja o desactivar una categoría o un oferente.

La verificación corre después de aplicar el cambio, dentro de la misma transacción, que se revierte con el 422.

### Bajas
Dar de baja un establecimiento publicado (DELETE, o PUT con `activo: false`) lo pasa a `OCULTO`. Reactivarlo no lo vuelve a publicar.

## Consecuencias
- Lo que ve el sitio público siempre cumple los requisitos.
- Para hacer cambios grandes en un publicado (p. ej. mudarlo de lugar a uno sin coordenadas) hay que despublicarlo, cambiar y volver a publicar.
- Los requisitos viven en `PublicacionService.faltantes`; agregar uno nuevo es un solo lugar del código.
