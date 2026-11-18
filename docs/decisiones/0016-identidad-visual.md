# 0016 · Identidad visual: "una ciudad entre ríos"

**Estado:** aceptada (noviembre 2026). Reemplaza la "paleta neutra de Material" de los ADR 0011 y 0012.

## Contexto
El sitio público y el panel usaban Angular Material con la paleta azul por defecto y Roboto: correcto, pero
sin identidad y fácil de confundir con cualquier otro sitio. El público es de turistas que llegan a Santa Fe y
vecinos que buscan planes; la tarea principal es encontrar qué hacer, dónde comer o dormir, y cómo llegar.
Los datos no tienen fotos, así que la identidad tiene que salir del color, la tipografía y un motivo gráfico.
Se decidió una identidad propia inspirada en la ciudad (no el manual de marca municipal) y compartida por los
dos frontends.

## Decisión
**Tokens compartidos** en `frontend/projects/shared/src/estilos/`:
- `_theme-colors.scss`: paletas M3 generadas con `ng generate @angular/material:theme-color` a partir del río.
- `_sicat.scss`: mixin `tema($densidad)` (Material + variables `--sicat-*`) y `base` (cuerpo, enlaces, foco
  visible, movimiento reducido). Los `styles.scss` de cada app solo lo incluyen (densidad 0 en el sitio, -1 en
  el panel).

**Color** (todas las combinaciones de texto pasan WCAG AA; la más baja es 5,05:1):

| Token | Valor | Uso |
|---|---|---|
| Cal | `#F4F6F5` | fondo, blanco frío de cal |
| Tinta | `#16262A` | texto; el menú del panel |
| Río | `#1D5C63` | primario: enlaces, botones, la banda de la portada |
| Limo | `#B5832A` (texto `#85600F`) | destacados |
| Camalote | `#3F6A2E` | publicado, verificado, circuitos |
| Ceibo | `#B3261E` | errores y el pin del lugar en el mapa; nada más |

**Tipografía:** Archivo (variable, Google Fonts), una sola familia con el ancho como recurso: 125 % en
titulares (remite a la cartelería del puerto, los silos y los molinos), 112 % en nombres de lugares, normal en
el texto y 87,5 % en las celdas de las tablas del panel.

**Motivo:** la silueta del Puente Colgante en línea (`shared/src/lib/puente.ts`), solo en la portada del sitio
y en el ingreso al panel. Es el único gesto llamativo; el resto queda sobrio: la lista de resultados es una
lista con filetes (no una grilla de tarjetas iguales) y la ficha usa secciones, con un solo panel lateral fijo
para "Dónde queda" y "Contacto".

**Movimiento:** el cable del puente se dibuja una vez al cargar la portada; del listado a la ficha el nombre
"viaja" (View Transitions del router, `withViewTransitions`; la ficha muestra el nombre que trae el listado
antes de que llegue la respuesta). Lo demás solo responde a acciones. Todo se apaga con
`prefers-reduced-motion`.

**Nombres en mayúsculas:** muchos vienen así de la importación. El sitio público los muestra en tipo título con
`nombreLegible()` (`shared/src/lib/textos.ts`), sin tocar los datos: respeta los que ya tienen minúsculas y
deja como están siglas con puntos, romanos y palabras sin vocales. Puede fallar en siglas pronunciables
("AMSAFE" → "Amsafe"); se corrige cargando el nombre bien en el panel.

**Caché del front:** `nginx.conf.template` revalida `index.html` siempre y guarda un año los bundles con hash,
para que cada versión llegue sin forzar la recarga.

## Consecuencias
- Cambiar la identidad (o adoptar la municipal más adelante) se hace en `shared/src/estilos/` y en el SVG del
  puente; las pantallas usan variables, no colores sueltos (salvo los popups de Leaflet, que son HTML armado).
- Se agrega una fuente web (Archivo variable, ~100 kB con todos los ejes) y se quita Roboto.
- La transición del nombre solo se ve en navegadores con View Transitions; en los demás, la navegación es la de
  siempre.
- Auditoría hecha sin dependencias externas: contraste de los tokens, árbol de accesibilidad (títulos, nombres
  de controles, `lang`, enlace para saltar al contenido) y foco visible con teclado.
