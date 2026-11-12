# SICAT SF · Modelo de datos

> Fuente de verdad del modelo para Claude Code. Transcripción fiel de los diagramas `SICAT_SF_DER_1de2.svg` y `SICAT_SF_DER_2de2.svg` (carpeta `docs/`). Si hay conflicto con otro documento, manda este.
> Convenciones: `PK` clave primaria, `FK` clave foránea, `UK` única, `NULL` opcional. Todos los ids son `bigint`.

## Esquemas

| Esquema | Contenido |
|---|---|
| `seguridad` | `rol`, `usuario` |
| `auditoria` | `auditoria` |
| `sicat` | todo el modelo de negocio |
| `publico` | **a crear**: solo vistas de lectura para la API pública (ver `ARQUITECTURA.md`) |

## Idea central del modelo

- **Oferente** es quien presta el servicio (persona/institución). Tiene **establecimientos**.
- **Establecimiento** es la entidad física y se ubica en un **lugar** (geografía, con jerarquía `id_lugar_padre`).
- **Oferta** es lo que un establecimiento ofrece, clasificado en una **categoría** jerárquica. Un establecimiento puede tener varias ofertas.
- Los datos propios de cada tipo de oferta viven en tablas de extensión 1 a 1 con `oferta`: `alojamiento`, `circuito`. Además `salon` y `visita_guiada` cuelgan de `oferta` (1 a N).
- Todo lo demás (museos, gastronomía, iglesias, etc.) se describe con **categoría** + **característica** (catálogo con `grupo`), sin tablas propias.

## Esquema `seguridad`

```
rol
  PK id_rol
  UK nombre            text
     descripcion       text

usuario
  PK id_usuario
  FK id_rol            -> rol
  FK id_oferente NULL  -> sicat.oferente      (reservado para la etapa 2: usuario oferente)
     nombre_completo   text
  UK email             text
     hash_password     text
     activo            boolean
     ultimo_acceso     timestamptz
```

## Esquema `auditoria`

```
auditoria
  PK id_auditoria
     tabla_afectada    text
     clave_registro    text
     accion            text   -- ALTA | MODIFICACION | ELIMINACION
  FK id_usuario NULL   -> seguridad.usuario
     fecha_hora        timestamptz
     origen            text   -- APP | MIGRACION | SISTEMA
     datos_anteriores  jsonb
     datos_nuevos      jsonb
```

## Esquema `sicat`

### Oferente y contacto

```
tipo_oferente
  PK id_tipo_oferente
  UK nombre            text
     activo            boolean

oferente
  PK id_oferente
  FK id_tipo_oferente  -> tipo_oferente
     nombre            text
     activo            boolean

tipo_canal
  PK id_tipo_canal
  UK nombre            text
     activo            boolean

canal_contacto
  PK id_canal
  FK id_establecimiento NULL -> establecimiento
  FK id_oferente        NULL -> oferente
  FK id_tipo_canal           -> tipo_canal
     valor             text
     uso               text
     es_publico        boolean     -- solo los true salen al sitio público
     orden             smallint
```

### Establecimiento

```
establecimiento
  PK id_establecimiento
  FK id_oferente       -> oferente
  FK id_lugar NULL     -> lugar
     nombre                 text
     descripcion            text
     anio_inauguracion      smallint
     estado_funcionamiento  text
     motivo_estado          text NULL
     fecha_reapertura       date NULL
     estado_publicacion     text        -- BORRADOR | PUBLICADO | OCULTO
     destacado              boolean
     activo                 boolean

referente
  PK id_referente
  FK id_establecimiento -> establecimiento
     nombre, cargo, telefono, correo   text
     activo                            boolean

distincion
  PK id_distincion
     nombre               text
     organismo_otorgante  text
     activo               boolean

establecimiento_distincion
  PK,FK id_establecimiento -> establecimiento
  PK,FK id_distincion      -> distincion
        anio_otorgamiento  smallint NULL
        url_verificacion   text NULL

establecimiento_caracteristica
  PK,FK id_establecimiento -> establecimiento
  PK,FK id_caracteristica  -> caracteristica
        cantidad           int NULL
```

### Lugar

```
tipo_lugar
  PK id_tipo_lugar
  UK nombre            text
     activo            boolean

localidad
  PK id_localidad
     nombre            text
     provincia         text
     activo            boolean

lugar
  PK id_lugar
  FK id_lugar_padre NULL -> lugar          (jerarquía: un lugar dentro de otro)
  FK id_tipo_lugar  NULL -> tipo_lugar
  FK id_localidad   NULL -> localidad
     nombre            text
     calle             text
     altura            int
     km                numeric(6,1)
     calle_cruce       text
     complemento       varchar(100)
     punto             geography(Point,4326)
     geometria         geography(Geometry,4326)
     estado_geo        text
```

### Oferta y clasificación

```
categoria
  PK id_categoria
  FK id_categoria_padre NULL -> categoria   (árbol)
     nombre            text
     activo            boolean

caracteristica
  PK id_caracteristica
     grupo             text
     nombre            text
     admite_cantidad   boolean
     activo            boolean

categoria_caracteristica          -- qué características aplican a cada categoría
  PK,FK id_categoria      -> categoria
  PK,FK id_caracteristica -> caracteristica

oferta
  PK id_oferta
  FK id_establecimiento -> establecimiento
  FK id_categoria       -> categoria        (categoría principal)
     acceso             text
     publico_admitido   text
     frecuencia         text
     fecha_relevamiento date
     activo             boolean

oferta_categoria                  -- categorías adicionales de una oferta
  PK,FK id_oferta    -> oferta
  PK,FK id_categoria -> categoria
```

### Especializaciones y visitas

```
alojamiento                       -- 1 a 1 con oferta
  PK,FK id_oferta -> oferta
        estrellas              smallint NULL
        cant_habitaciones      int
        total_plazas           int
        hab_accesibles         int
        plazas_accesibles      int
        personal_permanente    int
        personal_no_permanente int

salon                             -- N por oferta
  PK id_salon
  FK id_oferta -> oferta
     nombre                text
     cap_auditorio         int
     cap_banquete          int
     cap_coctel            int
     m2                    numeric
     estado_conservacion   text
     estado_funcionamiento text
     activo                boolean

salon_caracteristica
  PK,FK id_salon          -> salon
  PK,FK id_caracteristica -> caracteristica
        cantidad          int NULL

visita_guiada
  PK id_visita
  FK id_oferta -> oferta
     modalidad         text
     requiere_reserva  boolean
     habilitada        boolean

horario
  PK id_horario
  FK id_oferta         -> oferta
  FK id_visita NULL    -> visita_guiada     (si es horario de una visita puntual)
     dia_semana        smallint (1-7)
     hora_desde        time
     hora_hasta        time
```

### Circuitos

```
circuito                          -- 1 a 1 con oferta
  PK,FK id_oferta -> oferta
  FK id_lugar_partida NULL -> lugar
        duracion_min       int
        trazado            geography(LineString,4326)

circuito_parada
  PK,FK id_oferta   -> circuito
  PK    orden       smallint
  FK id_establecimiento -> establecimiento
```

## Relaciones resumidas

- `oferente 1—N establecimiento 1—N oferta`
- `oferta N—1 categoria` (principal) y `oferta N—M categoria` (vía `oferta_categoria`)
- `oferta 1—1 alojamiento`, `oferta 1—1 circuito`, `oferta 1—N salon`, `oferta 1—N visita_guiada`, `oferta 1—N horario`
- `establecimiento N—1 lugar`; `lugar N—1 lugar` (padre), `lugar N—1 tipo_lugar`, `lugar N—1 localidad`
- `establecimiento N—M caracteristica`, `salon N—M caracteristica`, `categoria N—M caracteristica`
- `establecimiento N—M distincion`
- `canal_contacto N—1 establecimiento` **o** `N—1 oferente` (uno de los dos, ambos opcionales)
- `usuario N—1 rol`; `usuario N—1 oferente` (opcional, etapa 2)
- `auditoria N—1 usuario` (opcional)
