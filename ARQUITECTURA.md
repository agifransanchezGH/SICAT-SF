# SICAT SF · Arquitectura, estructura del proyecto y endpoints

> Documento de referencia para Claude Code. Es la fuente de verdad sobre la arquitectura. El modelo de datos vive en `docs/modelo-datos.md` (diagrama en `docs/SICAT_SF_DER.drawio`); si los nombres de este documento difieren del modelo, **manda el modelo**. Ver `docs/decisiones/0003-alineacion-con-modelo-de-datos.md`.

## 1. Contexto

SICAT SF es el sistema de información turística de la Municipalidad de Santa Fe. Etapa 1: base relacional normalizada (migrada desde Excel), ABM para el personal municipal, consultas/filtros, vista de mapa y un sitio público de consulta.

Restricción central: **la parte pública no puede afectar a la administración**. Por eso son dos aplicaciones separadas, y la pública solo ve un esquema de vistas de solo lectura.

## 2. Stack

| Capa | Tecnología |
|---|---|
| Base de datos | PostgreSQL 17 + PostGIS 3.5 |
| Migraciones | Flyway (scripts SQL versionados) |
| Backend | Java 21 + Spring Boot 3.5 (Maven multi-módulo, `./mvnw`) |
| Seguridad | Spring Security + JWT (HS256), roles |
| Frontend | Angular 20 (workspace con dos apps: admin y público, y librería `shared`) |
| Mapa | Leaflet + OpenStreetMap |
| Documentación API | OpenAPI / Swagger (springdoc) |
| Entorno local | Docker Compose |
| Tests | JUnit 5, ArchUnit (reglas de arquitectura), Testcontainers (PostGIS real) |

## 3. Arquitectura

```
ZONA INTERNA (local)                    ZONA PÚBLICA (aislada)
Personal -> Angular Admin               Ciudadanos -> Angular Público
              |                                         |
        API Admin (rw)                            API Pública (ro)
              |                                         |
  esquemas sicat · seguridad · auditoria --publicación--> esquema publico (vistas)
                       PostgreSQL + PostGIS
```

Reglas de arquitectura (obligatorias):

1. La API Pública **solo** lee el esquema `publico` (vistas) con un usuario de BD de **solo lectura** (`sicat_public_ro`). Ese usuario no tiene permisos sobre `sicat`, `seguridad` ni `auditoria`.
2. La API Admin usa el usuario `sicat_admin_rw` y es la **única** que ejecuta migraciones Flyway.
3. `sicat-public-api` **nunca** depende de `sicat-admin-api`. Lo compartido vive en `sicat-core`. (Test ArchUnit.)
4. Ninguna entidad JPA se expone por HTTP: siempre DTOs.
5. La lógica de auditoría está separada de la lógica de negocio (paquete `auditoria`, sin referencias desde los paquetes de dominio). (Test ArchUnit.)
6. Los datos públicos se definen por **vista**, no por filtro en código: si una columna no está en la vista, no puede filtrarse.
7. Los datos se publican solo si el establecimiento está `PUBLICADO`, el registro está `activo` y los contactos tienen `es_publico = true`.

## 4. Estructura de carpetas

```
sicat-sf/
├── CLAUDE.md                      # instrucciones para Claude Code
├── ARQUITECTURA.md                # este documento
├── README.md
├── docker-compose.yml             # db (postgis) + api-admin + api-public + front-admin + front-public
├── .env.example
├── docs/
│   ├── modelo-datos.md            # fuente de verdad del modelo
│   ├── SICAT_SF_DER.drawio
│   ├── arquitectura.drawio
│   └── decisiones/                # ADRs breves
│
├── backend/
│   ├── pom.xml                    # POM padre (multi-módulo)
│   ├── mvnw, mvnw.cmd, .mvn/      # Maven wrapper
│   │
│   ├── sicat-core/                # compartido: entidades, repositorios, migraciones
│   │   └── src/main/
│   │       ├── java/ar/sf/sicat/core/
│   │       │   ├── seguridad/                 # Rol, Usuario                (esquema seguridad)
│   │       │   ├── auditoria/                 # Auditoria                   (esquema auditoria)
│   │       │   ├── dominio/                   # esquema sicat
│   │       │   │   ├── oferente/              # TipoOferente, Oferente
│   │       │   │   ├── contacto/              # TipoCanal, CanalContacto
│   │       │   │   ├── establecimiento/       # Establecimiento, Referente, Distincion, EstablecimientoDistincion,
│   │       │   │   │                          # EstablecimientoCaracteristica, EstadoPublicacion
│   │       │   │   ├── lugar/                 # TipoLugar, Localidad, Lugar (punto, geometria, id_lugar_padre)
│   │       │   │   ├── clasificacion/         # Categoria (árbol), Caracteristica, CategoriaCaracteristica
│   │       │   │   ├── oferta/                # Oferta, OfertaCategoria
│   │       │   │   ├── horario/               # Horario
│   │       │   │   └── especializacion/
│   │       │   │       ├── alojamiento/       # 1 a 1 con oferta
│   │       │   │       ├── circuito/          # 1 a 1 con oferta + CircuitoParada
│   │       │   │       ├── salon/             # N por oferta + SalonCaracteristica
│   │       │   │       └── visitaguiada/      # N por oferta
│   │       │   ├── vistapublica/              # entidades @Immutable mapeadas a vistas de `publico`
│   │       │   └── comun/                     # PaginaDto, excepciones, utilidades geo
│   │       └── resources/db/migration/        # V1__esquemas.sql, V2__..., etc.
│   │
│   ├── sicat-admin-api/           # API de administración (rw, interna)
│   │   └── src/main/
│   │       ├── java/ar/sf/sicat/admin/
│   │       │   ├── AdminApplication.java
│   │       │   ├── config/                    # seguridad, CORS, OpenAPI, manejo de errores RFC 7807
│   │       │   ├── seguridad/                 # login, JWT, usuarios, roles
│   │       │   ├── auditoria/                 # listener + consulta
│   │       │   ├── catalogo/                  # tipos, localidades, categorías, características, distinciones
│   │       │   ├── oferente/                  # controller, service, dto, mapper
│   │       │   ├── establecimiento/           # incluye referentes, distinciones, características
│   │       │   ├── lugar/
│   │       │   ├── contacto/
│   │       │   ├── oferta/                    # incluye categorías adicionales y horarios
│   │       │   ├── especializacion/           # alojamiento, circuito, salon, visitaguiada
│   │       │   ├── publicacion/               # publicar/despublicar, validaciones
│   │       │   ├── mapa/                      # GeoJSON interno
│   │       │   └── importacion/               # migración desde Excel (carga masiva)
│   │       └── resources/application.yml      # Flyway HABILITADO
│   │
│   └── sicat-public-api/          # API pública (ro, expuesta)
│       └── src/main/
│           ├── java/ar/sf/sicat/publica/
│           │   ├── PublicApplication.java
│           │   ├── config/                    # CORS, caché (Caffeine), rate limit (Bucket4j), solo GET, OpenAPI
│           │   ├── salud/                     # /health
│           │   ├── establecimiento/           # controller, service, dto (solo GET)
│           │   ├── catalogo/
│           │   └── mapa/                      # GeoJSON público
│           └── resources/application.yml      # Flyway DESHABILITADO
│
├── frontend/                      # Angular workspace
│   ├── angular.json
│   ├── package.json
│   └── projects/
│       ├── admin/                 # app panel municipal
│       │   └── src/app/
│       │       ├── core/          # auth, interceptors, guards por rol
│       │       ├── layout/
│       │       ├── oferentes/
│       │       ├── establecimientos/
│       │       ├── ofertas/
│       │       ├── especializaciones/ # alojamiento, circuito, salón, visita guiada
│       │       ├── catalogos/     # incluye categorías y características
│       │       ├── lugares/
│       │       ├── mapa/
│       │       └── auditoria/
│       ├── public/                # app sitio público
│       │   └── src/app/
│       │       ├── core/
│       │       ├── listado/       # buscador + filtros
│       │       ├── ficha/         # bloque común + ofertas con su detalle
│       │       └── mapa/
│       └── shared/                # librería: modelos TS, componentes UI, utilidades
│
└── infra/
    ├── postgres/init/             # crea roles sicat_admin_rw y sicat_public_ro + postgis
    └── nginx/                     # reverse proxy (opcional)
```

## 5. Base de datos: esquemas y permisos

| Esquema | Contenido |
|---|---|
| `seguridad` | `rol`, `usuario` |
| `auditoria` | `auditoria` |
| `sicat` | todo el modelo de negocio (ver `docs/modelo-datos.md`) |
| `publico` | solo vistas de lectura |

Vistas de `publico` (propuesta inicial en `V8__vistas_publicas.sql`, a revisar):

- `v_establecimiento_publico`: id, nombre, descripción, año de inauguración, estado de funcionamiento, destacado, oferente, dirección (calle, altura, km, cruce, complemento), localidad, provincia, lugar, `lat`/`lng` desde `lugar.punto`.
- `v_oferta_publica`: ofertas activas con categoría principal, acceso, público admitido, frecuencia.
- `v_oferta_categoria_publica`: categorías adicionales.
- `v_contacto_publico`: canales con `es_publico = true` (del establecimiento o de su oferente).
- `v_horario_publico`, `v_alojamiento_publico`, `v_salon_publico`, `v_visita_guiada_publica`, `v_circuito_publico`, `v_circuito_parada_publica`.
- `v_caracteristica_publica`: características del establecimiento con grupo y cantidad.
- `v_distincion_publica`.
- `v_categoria_publica`: categorías activas (árbol) para filtros.

Permisos:

- `sicat_admin_rw`: dueño de los esquemas y objetos (ejecuta Flyway).
- `sicat_public_ro`: `USAGE` en `publico` y `SELECT` sobre sus vistas. Nada más.

Geografía: `lugar.punto geography(Point,4326)`, `lugar.geometria geography(Geometry,4326)`, `circuito.trazado geography(LineString,4326)`, con índices GIST.

Publicación: `establecimiento.estado_publicacion` (`BORRADOR`, `PUBLICADO`, `OCULTO`). Las vistas filtran `PUBLICADO`.

## 6. Roles

| Rol | Permisos |
|---|---|
| `ADMIN` | Todo, incluida gestión de usuarios y auditoría |
| `CARGADOR` | ABM de oferentes, establecimientos, ofertas, lugares, contactos y especializaciones; puede publicar |
| `CONSULTA` | Solo lectura en el panel admin |
| `OFERENTE` | **Etapa 2.** Solo sus propios establecimientos (`usuario.id_oferente` ya previsto; no implementar aún) |

## 7. Convenciones de API

- JSON, UTF-8. Nombres de campos en `camelCase`; dominio en español.
- Versionado en la ruta: `/api/admin/v1` y `/api/public/v1`.
- Paginación: `?page=0&size=20&sort=nombre,asc`. Respuesta: `{ contenido, pagina, tamanio, totalElementos, totalPaginas }` (`PaginaDto` en `sicat-core`).
- Errores en formato RFC 7807 (`application/problem+json`).
- Fechas en ISO 8601. Coordenadas como `{ "lat": -31.63, "lng": -60.70 }`; mapas como GeoJSON (`FeatureCollection`).
- Todo endpoint admin requiere `Authorization: Bearer <jwt>`, salvo `/auth/login`.
- La API pública no tiene autenticación; solo acepta `GET`.

## 8. Endpoints: API Admin (`/api/admin/v1`)

Convenciones de los ABM (detalle en `docs/decisiones/0007-convenciones-abm.md`):

- **Bajas lógicas** (`DELETE` → `activo = false`) en catálogos, categorías, oferentes, establecimientos, ofertas y salones. Los listados omiten inactivos salvo `?incluirInactivos=true`; un `PUT` con `"activo": true` reactiva.
- **Bajas físicas** solo en lugares (409 si está en uso) y visitas guiadas (junto con sus horarios).
- **`PUT` de listas** (contactos, referentes, horarios, distinciones, características, paradas, categorías): reemplazan la lista completa. Ítems con `id` se modifican, sin `id` se crean y los que no vienen se eliminan.
- **`bbox`** = `minLng,minLat,maxLng,maxLat`. **Geometrías** en GeoJSON (`[lng, lat]`).
- Filtro **`categoria`** de establecimientos: incluye subcategorías y categorías adicionales de ofertas activas.

### Autenticación y usuarios
| Método | Ruta | Descripción | Rol |
|---|---|---|---|
| POST | `/auth/login` | `{ email, password }` → `{ accessToken, refreshToken, tipo, expiraEn, usuario }` | público |
| POST | `/auth/refresh` | `{ refreshToken }` → misma respuesta que login | con refresh token |
| GET | `/auth/me` | Usuario y rol actuales | autenticado |
| GET | `/usuarios` | Lista usuarios (paginado) | ADMIN |
| POST | `/usuarios` | Crea usuario (email repetido → 409) | ADMIN |
| PUT | `/usuarios/{id}` | Modifica usuario y rol; `password` opcional | ADMIN |
| PATCH | `/usuarios/{id}/estado` | `{ activo }` Activa/desactiva | ADMIN |

Tokens, refresh sin estado y primer ADMIN: ver `docs/decisiones/0005-refresh-token-y-admin-inicial.md`.

### Catálogos
`{nombre}` ∈ `tipo-oferente`, `tipo-canal`, `tipo-lugar`, `localidad`, `distincion`, `caracteristica`.

| Método | Ruta | Descripción | Rol |
|---|---|---|---|
| GET | `/catalogos/{nombre}` | Lista un catálogo | todos |
| POST | `/catalogos/{nombre}` | Crea ítem | ADMIN |
| PUT | `/catalogos/{nombre}/{id}` | Modifica ítem | ADMIN |
| DELETE | `/catalogos/{nombre}/{id}` | Baja lógica (`activo = false`) | ADMIN |

### Categorías (árbol) y características por categoría
| Método | Ruta | Descripción | Rol |
|---|---|---|---|
| GET | `/categorias` | Árbol de categorías (filtro `padreId`) | todos |
| POST | `/categorias` | Crea (con `padreId`) | ADMIN |
| PUT | `/categorias/{id}` | Modifica | ADMIN |
| DELETE | `/categorias/{id}` | Baja lógica | ADMIN |
| GET | `/categorias/{id}/caracteristicas` | Características que aplican a la categoría | todos |
| PUT | `/categorias/{id}/caracteristicas` | Reemplaza la lista | ADMIN |

### Oferentes
| Método | Ruta | Descripción | Rol |
|---|---|---|---|
| GET | `/oferentes` | Lista con filtros (`q`, `tipo`) y paginación | todos |
| GET | `/oferentes/{id}` | Detalle con sus establecimientos | todos |
| POST | `/oferentes` | Crea | ADMIN, CARGADOR |
| PUT | `/oferentes/{id}` | Modifica | ADMIN, CARGADOR |
| DELETE | `/oferentes/{id}` | Baja lógica | ADMIN |
| GET | `/oferentes/{id}/contactos` | Canales de contacto del oferente | todos |
| PUT | `/oferentes/{id}/contactos` | Reemplaza canales (con `esPublico`) | ADMIN, CARGADOR |

### Establecimientos
| Método | Ruta | Descripción | Rol |
|---|---|---|---|
| GET | `/establecimientos` | Lista con filtros: `q`, `categoria`, `estado`, `oferenteId`, `localidadId`, `bbox`, paginación | todos |
| GET | `/establecimientos/{id}` | Detalle completo (datos comunes + ofertas) | todos |
| POST | `/establecimientos` | Crea | ADMIN, CARGADOR |
| PUT | `/establecimientos/{id}` | Modifica | ADMIN, CARGADOR |
| DELETE | `/establecimientos/{id}` | Baja lógica | ADMIN |
| GET | `/establecimientos/{id}/publicacion` | Estado y requisitos que faltan para publicar | todos |
| POST | `/establecimientos/{id}/publicar` | Pasa a `PUBLICADO` si cumple los requisitos (422 con `errores` si no) | ADMIN, CARGADOR |
| POST | `/establecimientos/{id}/despublicar` | Pasa a `OCULTO` (422 si está en `BORRADOR`) | ADMIN, CARGADOR |
| GET | `/establecimientos/duplicados` | Grupos de establecimientos activos con el mismo nombre normalizado (primero los de igual dirección) | ADMIN, CARGADOR |
| GET / PUT | `/establecimientos/{id}/contactos` | Canales de contacto (PUT reemplaza) | todos / ADMIN, CARGADOR |
| GET / PUT | `/establecimientos/{id}/referentes` | Referentes | todos / ADMIN, CARGADOR |
| GET / PUT | `/establecimientos/{id}/distinciones` | Distinciones con año y URL | todos / ADMIN, CARGADOR |
| GET / PUT | `/establecimientos/{id}/caracteristicas` | Características con cantidad | todos / ADMIN, CARGADOR |

### Ofertas
| Método | Ruta | Descripción | Rol |
|---|---|---|---|
| GET | `/establecimientos/{id}/ofertas` | Ofertas del establecimiento | todos |
| POST | `/establecimientos/{id}/ofertas` | Crea oferta (categoría principal obligatoria) | ADMIN, CARGADOR |
| GET | `/ofertas/{id}` | Detalle con especialización, horarios y categorías | todos |
| PUT | `/ofertas/{id}` | Modifica | ADMIN, CARGADOR |
| DELETE | `/ofertas/{id}` | Baja lógica | ADMIN, CARGADOR |
| GET / PUT | `/ofertas/{id}/categorias` | Categorías adicionales | todos / ADMIN, CARGADOR |
| GET / PUT | `/ofertas/{id}/horarios` | Horarios (PUT reemplaza) | todos / ADMIN, CARGADOR |

### Especializaciones (bajo la oferta)
| Método | Ruta | Descripción | Rol |
|---|---|---|---|
| GET / PUT | `/ofertas/{id}/alojamiento` | Datos de alojamiento (PUT crea o reemplaza, 1 a 1) | todos / ADMIN, CARGADOR |
| GET / PUT | `/ofertas/{id}/circuito` | Datos del circuito, trazado GeoJSON | todos / ADMIN, CARGADOR |
| GET / PUT | `/ofertas/{id}/circuito/paradas` | Paradas ordenadas | todos / ADMIN, CARGADOR |
| GET / POST | `/ofertas/{id}/salones` | Salones de la oferta | todos / ADMIN, CARGADOR |
| PUT / DELETE | `/salones/{id}` | Modifica / baja lógica | ADMIN, CARGADOR |
| GET / PUT | `/salones/{id}/caracteristicas` | Características del salón | todos / ADMIN, CARGADOR |
| GET / POST | `/ofertas/{id}/visitas-guiadas` | Visitas guiadas | todos / ADMIN, CARGADOR |
| PUT / DELETE | `/visitas-guiadas/{id}` | Modifica / elimina | ADMIN, CARGADOR |
| GET / PUT | `/visitas-guiadas/{id}/horarios` | Horarios de la visita | todos / ADMIN, CARGADOR |

### Lugares
| Método | Ruta | Descripción | Rol |
|---|---|---|---|
| GET | `/lugares` | Lista (filtro `tipoLugar`, `padreId`, `localidadId`, `bbox`) | todos |
| GET | `/lugares/{id}` | Detalle con lugares hijos | todos |
| POST | `/lugares` | Crea (incluye coordenadas, geometría opcional y `padreId`) | ADMIN, CARGADOR |
| PUT | `/lugares/{id}` | Modifica | ADMIN, CARGADOR |
| DELETE | `/lugares/{id}` | Elimina si no está referenciado | ADMIN |

### Mapa
| Método | Ruta | Descripción | Rol |
|---|---|---|---|
| GET | `/mapa/establecimientos` | GeoJSON con filtros `categoria`, `estado`, `bbox` | todos |
| GET | `/mapa/establecimientos/cercanos` | Por `lat`, `lng`, `radioMetros` | todos |
| GET | `/mapa/circuitos` | Trazados de circuitos (GeoJSON) | todos |

### Importación (migración desde Excel)
| Método | Ruta | Descripción | Rol |
|---|---|---|---|
| GET | `/importacion/perfiles` | Planillas que se pueden importar (`archivo`) y sus hojas → categorías | ADMIN |
| POST | `/importacion/{archivo}/validar` | Multipart (`archivo`=.xlsx). Simula y devuelve cada establecimiento como NUEVO, DUPLICADO_EN_BASE, DUPLICADO_EN_ARCHIVO o ERROR, sin guardar | ADMIN |
| POST | `/importacion/{archivo}/ejecutar` | Guarda los NUEVO en BORRADOR (auditoría con `origen = MIGRACION`); a los DUPLICADO_EN_BASE les suma la categoría si no la tienen | ADMIN |
| POST | `/importacion/geocodificar?limite=` | Geocodifica en segundo plano (OpenStreetMap, 1 por segundo) las direcciones pendientes | ADMIN |
| GET | `/importacion/geocodificacion` | Avance de la geocodificación | ADMIN |

`{archivo}` ∈ `gastronomia`, `alojamiento`, `museos`, `salones`, `iglesias`, `edificios-historicos`, `atractivos-naturales`, `espacios-culturales`, `recreacion`, `servicios-turisticos`. Detalle en `docs/decisiones/0010-importacion-desde-excel.md`.

### Auditoría (solo lectura)
| Método | Ruta | Descripción | Rol |
|---|---|---|---|
| GET | `/auditoria` | Paginado. Filtros: `tabla` (con o sin esquema), `clave`, `usuario` (id), `accion`, `origen`, `desde`, `hasta` (`yyyy-MM-dd`, hora de Argentina) | ADMIN |
| GET | `/auditoria/{tabla}/{clave}` | Historial de un registro, del más reciente al más antiguo | ADMIN, CARGADOR |

Qué se registra y cómo: ver `docs/decisiones/0006-auditoria-con-eventos-de-hibernate.md`. Los cambios hechos con SQL directo no se auditan.

## 9. Endpoints: API Pública (`/api/public/v1`)

Solo `GET`. Sin autenticación. Caché de 5 minutos (un cambio de publicación puede tardar hasta 5 minutos en verse) y 120 pedidos por minuto por IP. Detalle en `docs/decisiones/0009-api-publica.md`.

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/establecimientos` | Listado paginado (destacados primero). Filtros: `q` (nombre y categorías, sin acentos), `categoria` (incluye subcategorías y adicionales), `localidad`, `bbox` |
| GET | `/establecimientos/{id}` | Ficha: bloque común + `ofertas[]` con horarios y `detalle`. 404 si no está publicado |
| GET | `/establecimientos/cercanos` | Por `lat`, `lng`, `radioMetros` (por defecto 1000, máx. 20000) y `limite` (por defecto 20, máx. 100); incluye `distanciaMetros` |
| GET | `/categorias` | Árbol de categorías activas |
| GET | `/localidades` | Localidades con establecimientos publicados (para el filtro) |
| GET | `/mapa` | GeoJSON (puntos) de establecimientos publicados; mismos filtros que el listado |
| GET | `/mapa/circuitos` | GeoJSON (líneas) de circuitos publicados, con sus paradas |
| GET | `/health` | Estado del servicio |

Formato de la ficha:

```json
{
  "id": 12,
  "nombre": "…",
  "descripcion": "…",
  "direccion": "…",
  "localidad": "Santa Fe",
  "coordenadas": { "lat": -31.63, "lng": -60.70 },
  "contactos": [ { "tipo": "INSTAGRAM", "valor": "…" } ],
  "caracteristicas": [ { "grupo": "Servicios", "nombre": "Wi-Fi", "cantidad": null } ],
  "distinciones": [ { "nombre": "…", "anio": 2024 } ],
  "ofertas": [
    {
      "id": 30,
      "categoria": "Hotel",
      "categoriasAdicionales": [ "…" ],
      "horarios": [ { "diaSemana": 1, "desde": "08:00", "hasta": "20:00" } ],
      "detalle": { }
    }
  ]
}
```

`detalle` contiene solo los campos públicos de la especialización de la oferta (alojamiento, circuito, salones, visitas guiadas), o está vacío si la oferta no tiene especialización.

## 10. Orden de construcción sugerido

1. **Base** ✅: `infra/postgres/init` (roles) + migraciones Flyway del modelo en `sicat-core`.
2. **Core** ✅: entidades, repositorios y utilidades geo.
3. **Admin API – seguridad** ✅: login, JWT, roles, usuarios (+ login en el front admin).
4. **Admin API – auditoría** ✅: listener y tabla, antes de los ABM para que todo quede registrado.
5. **Admin API – ABM** ✅: catálogos, categorías/características, oferentes, lugares, establecimientos, ofertas, contactos, horarios, especializaciones. (Quedan para su paso: publicar/despublicar → 6, duplicados → 8, mapa → 10.)
6. **Publicación** ✅: estados y validaciones (requisitos en `docs/decisiones/0008-requisitos-de-publicacion.md`).
7. **Vistas públicas** + **API Pública** ✅ con caché y rate limit (campos públicos revisados; `V9` agrega la vista de búsqueda).
8. **Importación** desde Excel ✅ (validación, duplicados, ejecución y geocodificación con revisión manual).
9. **Frontend admin** (ABM, filtros) y **frontend público** (listado, ficha).
10. **Mapa** (admin y público).
11. **Etapa 2** (fuera de alcance): rol `OFERENTE`.

## 11. Reglas para Claude Code

- Antes de crear tablas o entidades, leer `docs/modelo-datos.md`. No inventar columnas.
- Todo cambio de esquema se hace con una **nueva migración** Flyway (`V<n>__descripcion.sql`); nunca editar una migración ya aplicada.
- No exponer entidades JPA: usar DTOs y mappers.
- No agregar dependencias del módulo público hacia el admin.
- Nombres de dominio en español; código de infraestructura en inglés técnico estándar.
- Cada endpoint nuevo necesita su DTO, validaciones (`jakarta.validation`), documentación OpenAPI y al menos un test.
- Un campo nuevo se vuelve público solo si se agrega **explícitamente** a la vista correspondiente.
- Ante una duda de diseño no cubierta aquí, preguntar antes de decidir.
