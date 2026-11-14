# 0010 · Importación desde las planillas Excel

- Estado: Aceptado
- Fecha: 2026-11-14

## Contexto
La oferta turística está en 11 planillas de la Dirección de Turismo (35 hojas, ~770 establecimientos) con columnas distintas por archivo, valores múltiples en una celda, teléfonos guardados como números, direcciones en texto libre y **sin coordenadas**. El "Modelo de datos normalizado" (v2.0) define categorías, subcategorías y variables.

## Decisión

### Endpoints
`POST /importacion/{archivo}/validar` (simula) y `/ejecutar` (guarda), con la planilla como `multipart`. `{archivo}` es un **perfil** por planilla fuente (`GET /importacion/perfiles`) que indica, por hoja, la categoría y subcategoría de destino y cómo leer sus columnas.

### Mapeo
- **Categorías**: categoría raíz por planilla y subcategoría por hoja, según el modelo normalizado (p. ej. Recreación › Cine, Servicios turísticos › Transfer, Turismo náutico y deportivo › Club náutico). En gastronomía la subcategoría sale de la columna `categoria`; en salones, de `CATEGORIZACIÓN`; en alojamiento, del tipo (Hotel, Delegación y complejo social, etc.). Se crean si no existen.
- **Columnas comunes** (reconocidas por el encabezado, sin importar acentos ni mayúsculas): nombre, dirección, teléfono, celular, WhatsApp, correo, web, Instagram (solo el usuario), Facebook, funcionamiento, entrada/acceso, visitas guiadas, descripción, dominio, fecha de relevamiento, año de inauguración.
- **Contactos**: todos públicos (las planillas son la guía turística). Celdas con varios valores se separan ("a / b", saltos de línea; teléfonos también "a/b"). Los datos del referente van a `referente` (no públicos).
- **Fichas técnicas** (hoteles, delegaciones, salones): cada lista (servicios, equipamiento, accesibilidad, sustentabilidad, medios de pago, reservas, tipos de habitación…) pasa a **características agrupadas**; los "Sí" de columnas no reconocidas también. Capacidades, estrellas y personal van a `alojamiento`; cada fila de salones es un `salon` y las filas del mismo establecimiento se agrupan.
- **Nada se pierde**: un texto en una columna no reconocida se agrega a la descripción con su encabezado como etiqueta.
- **Oferente**: uno por establecimiento, con el mismo nombre y tipo "Sin clasificar" (se pueden unificar después).
- Todo queda en **BORRADOR**, con el lugar en la localidad Santa Fe y `estado_geo = PENDIENTE`.
- Corrección de datos: calles con nombre de fecha que la planilla convirtió a fecha en inglés ("4 de January 0895" → "4 de Enero 895").

### Duplicados
- Clave: nombre normalizado + dirección normalizada (si alguno no tiene dirección, alcanza el nombre). Sucursales con distinta dirección no son duplicados.
- `DUPLICADO_EN_ARCHIVO`: se repite dentro de la misma planilla (se importa una vez).
- `DUPLICADO_EN_BASE`: ya existe. No se crea; si viene de otra planilla con otra categoría, **se le agrega como categoría adicional** (p. ej. un museo que también es "Atractivo turístico destacado").
- Reimportar la misma planilla no crea nada nuevo.
- `GET /establecimientos/duplicados` lista los grupos con el mismo nombre para revisarlos a mano.

### Geocodificación
- `POST /importacion/geocodificar?limite=N&estados=...` corre en segundo plano con **Nominatim (OpenStreetMap)**, como máximo una consulta por segundo (su política de uso), User-Agent "SICAT-SF/0.1 (Municipalidad de Santa Fe)", restringida a Santa Fe, Santo Tomé y la costa. Por defecto procesa `PENDIENTE`; con `estados=NO_ENCONTRADO,APROXIMADO` se reintentan. Nunca toca los `VERIFICADO`.
- Por dirección, hasta tres intentos: búsqueda estructurada "número + calle" con abreviaturas expandidas ("Bv." → "Bulevar", "Gral." → "General", "Av." → "Avenida"...), búsqueda libre con la dirección limpia (sin paréntesis, local, piso, "N°") y búsqueda libre con el texto original.
- Resultado por lugar: `AUTOMATICO` (encontrado con su número), `APROXIMADO` (se pidió un número pero se encontró la calle o la zona: p. ej. todo un bulevar en un punto) o `NO_ENCONTRADO` (ubicar a mano). Cada lugar se guarda en su propia transacción.
- **Las coordenadas automáticas hay que verificarlas**: un establecimiento con lugar `AUTOMATICO` o `APROXIMADO` no se puede publicar hasta que se confirme en el mapa (`estadoGeo = VERIFICADO`). Se agregó como requisito de publicación (amplía el ADR 0008).
- Se envían a OpenStreetMap solo direcciones de establecimientos (datos públicos), nunca datos de personas.

### Resultado con las planillas reales
767 establecimientos leídos, 735 creados, 31 que aparecían en dos planillas (se les sumó la categoría), 1 repetido dentro de gastronomía, 0 errores. En una muestra de 15 direcciones, OpenStreetMap encontró 13; las direcciones vagas ("Dique I - Puerto de Santa Fe", "Calle del Sol 2635") quedan para ubicar a mano.

## Consecuencias
- La importación es repetible y no duplica.
- Hay que revisar en el panel: ubicación en el mapa, posibles duplicados con nombres parecidos, oferentes a unificar y textos que quedaron en la descripción.
- La geocodificación de todo lleva ~12 minutos (≈660 direcciones a 1 por segundo).
- La importación escribe por JPA, así que queda auditada (ADR 0006).
