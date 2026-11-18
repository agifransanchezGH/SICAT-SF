# 0011 · Frontend del panel admin

- Estado: Aceptado
- Fecha: 2026-11-14

## Contexto
El paso 9 del orden de construcción pide el frontend admin (ABM y filtros) sobre la API Admin ya terminada (pasos 3 a 8). Hacía falta decidir la librería de componentes, la identidad visual y algunas convenciones de las pantallas.

## Decisión

### Librería y estilo
- **Angular Material 20 (M3)**, componentes standalone y señales.
- **Identidad compartida con el sitio público** (ADR 0016): tema y variables `--sicat-*` en `projects/shared/src/estilos/`, con densidad -1 para el panel.
- Formularios en `outline`, locale `es-AR`, paginador en español.

### Estructura de pantallas
- **Establecimientos** es la entrada: listado con filtros (texto, estado, categoría, localidad, bajas) y ficha con pestañas: Datos, Publicación, Ofertas, Contactos, Características, Referentes, Distinciones e Historial.
- **Ofertas** se abren desde la ficha del establecimiento. Las especializaciones (alojamiento, circuito, salones, visitas guiadas) son pestañas de la oferta: se muestran las que tiene y se agregan desde "+ Especialización".
- **Lugares y ubicaciones**: por defecto muestra la cola **"A revisar"** (`estadoGeo` AUTOMATICO, APROXIMADO, NO_ENCONTRADO o PENDIENTE). Una ubicación correcta se confirma con un clic (pasa a VERIFICADO); si no, se edita y se cargan latitud y longitud a mano. Coordenadas escritas por una persona quedan VERIFICADO. Desde el paso 10 la ubicación se marca también sobre el mapa (ADR 0013).
- Además: Oferentes, Posibles duplicados, Catálogos, Categorías (árbol), Importación (validar → importar → geocodificar), Usuarios y Auditoría.

### Convenciones
- Los filtros del listado de establecimientos viven en la URL (`?q=…&estado=…&pagina=…`): al volver de una ficha se conserva la búsqueda.
- Las listas hijas (contactos, horarios, referentes, distinciones, características, paradas) se editan completas y se guardan con un PUT de la lista, como define ADR 0007.
- Los errores RFC 7807 se muestran con `detail` y la lista `errores` (p. ej. los requisitos de publicación que faltan).
- Permisos en la UI según el rol (los valida igual la API): CONSULTA solo lee; CARGADOR carga y publica; ADMIN además da de baja, administra catálogos, usuarios, importación y auditoría.
- La API Admin suma dos cosas para el panel: el filtro `estadoGeo` en `GET /lugares` (uno o varios) y el campo `estadoGeo` en el resumen de establecimientos.

## Consecuencias
- El panel se puede usar de punta a punta para cargar, revisar y publicar lo importado.
- El bundle inicial ronda 750 kB por Material; el presupuesto del admin se subió a 1 MB (aviso) / 1,5 MB (error). Es una herramienta interna.
- El sitio público quedó en ADR 0012 y los mapas en ADR 0013.
