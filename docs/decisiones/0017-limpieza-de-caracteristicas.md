# 0017 · Características: selector usable, limpieza del catálogo e importador que no duplica

**Estado:** aceptada (noviembre 2026).

## Contexto
El catálogo tiene 354 características en 19 grupos. El selector del panel era una grilla de casillas donde los
títulos de grupo se confundían con las opciones. La importación dejó duplicados por tipeo, mayúsculas o plural
("Grifo monocomando/monomando", tres "Uso de lenguaje claro…"), direcciones cargadas como característica ("Por
calle Lavaisse 610") y entradas pegadas ("Servicio medico.Estacionamiento"). Una consulta de solo lectura encontró
53 conjuntos de posibles duplicados.

## Decisión
**Selector** (`admin/src/app/comun/caracteristicas-editor.ts`):
- Buscador sin acentos ni mayúsculas (`normalizar()` en `shared/src/lib/textos.ts`, misma regla que
  `Texto.clave`), resumen fijo con fichas "×", "Ver solo seleccionadas" y contador en `aria-live`.
- El modo depende del ancho del componente (no de la ventana): desde 900 px, panel de grupos con contadores a la
  izquierda y el grupo elegido a la derecha; más angosto (celular, dentro de un salón), grupos plegables que
  arrancan abiertos si tienen selección.
- Cada grupo es un `fieldset` con `legend`; filas clicables de 44 px; casilla nativa (Espacio la tilda); orden
  alfabético; si el foco cae debajo de la cabecera fija, se lo trae a la vista (WCAG 2.4.11).
- "Hay cambios" compara con lo guardado: tildar y destildar no cuenta.
- Tarjetas y pestañas usan `overflow: clip` (no crea contenedor de scroll), para que `position: sticky` funcione.

**Limpieza** (`/api/admin/v1/caracteristicas/…`, solo ADMIN; pantalla Catálogos › Características › Limpiar
duplicados):
- *Fusionar* varias del mismo grupo: los usos (establecimientos, salones, categorías) pasan a la que queda sin
  duplicar asignaciones; si la que queda no tenía cantidad, toma la de la absorbida. Las absorbidas **se borran**
  (no se dan de baja: ya no tienen usos y el catálogo queda limpio).
- *Separar* una entrada pegada en varias del mismo grupo (crea o reutiliza cada parte) con los mismos usos.
- *Pasar a notas internas* lo que no es una característica (mismo criterio que el ADR 0015). 409 si la usan
  salones o categorías, que no tienen notas.
- Todo por JPA: la auditoría registra cada asignación movida y cada característica borrada. No hay SQL directo
  ni cambio de esquema.
- Las sugerencias de la pantalla (parecidas y "¿dirección?") se calculan en el cliente; la decisión es de quien
  administra: "Rampas" y "Rampas con asistencia" pueden no ser lo mismo.

**Importador** (`importacion/lectura/Texto.java`, `Registro`, `Interprete`, `ImportacionService`):
- `normalizarCaracteristica`: sin puntuación suelta al principio o al final, espacios simples, paréntesis sin
  espacios internos, TODO MAYÚSCULAS a tipo oración.
- `mismaCaracteristica`: misma clave, o Levenshtein ≥ 0,85 sobre la clave, salvo nombres de menos de 8 letras,
  números distintos ("cap. 200"/"cap. 700") o una negación de diferencia ("climatizada"/"no climatizada"). Sobre
  el catálogo real junta 39 pares, todos duplicados de verdad.
- Antes de crear una característica se reutiliza una "misma" del grupo; las direcciones (`pareceDireccion`) van a
  notas internas.

## Consecuencias
- Las fusiones las ejecuta el equipo desde la pantalla; el sistema no fusiona solo los datos existentes.
- La regla de "dirección" es heurística: puede marcar algo como "¿Dirección?" que no lo es (solo es una marca, no
  actúa sola) y en la importación manda a notas internas textos con calle y altura.
- Rendimiento: con 350 opciones visibles el filtrado tarda 50–130 ms; no se virtualizó (se perdería la búsqueda
  del navegador y complica el foco). Si el catálogo supera ~1500 opciones, revisar.
