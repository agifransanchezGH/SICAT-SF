# 0003 · Alineación de ARQUITECTURA.md con el modelo de datos

- Estado: Aceptado
- Fecha: 2026-11-12

## Contexto
La primera versión de `ARQUITECTURA.md` se escribió antes de cerrar el modelo. `docs/modelo-datos.md` es la fuente de verdad y difiere en varios puntos.

## Decisión
Se ajustó `ARQUITECTURA.md` al modelo:

| Antes | Ahora (según el modelo) |
|---|---|
| Esquema `negocio` | Esquemas `seguridad`, `auditoria`, `sicat` (+ `publico` para vistas) |
| Una tabla de especialización por tipo (10 tipos) | Solo `alojamiento` y `circuito` (1–1 con `oferta`), `salon` y `visita_guiada` (1–N). El resto se describe con `categoria` + `caracteristica` |
| Establecimiento → especialización | Establecimiento → `oferta` (clasificada por `categoria`) → especialización |
| `LUGAR.ubicacion` | `lugar.punto` (Point) y `lugar.geometria` (Geometry); `circuito.trazado` (LineString) |
| Horarios del establecimiento | Horarios de la `oferta` (y opcionalmente de una `visita_guiada`) |
| Endpoints `/establecimientos/{id}/{tipo}` | `/establecimientos/{id}/ofertas` y subrecursos bajo `/ofertas/{id}` |

## Consecuencias
- Agregar un tipo nuevo (p. ej. "bodega") normalmente no requiere tablas: alcanza con categorías y características.
- Solo se crea tabla de extensión cuando un tipo tiene datos estructurados propios (como alojamiento o circuito).
