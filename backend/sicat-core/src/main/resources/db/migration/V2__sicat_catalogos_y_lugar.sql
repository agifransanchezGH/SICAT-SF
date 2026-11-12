-- SICAT SF · V2 · Catálogos, clasificación y lugares (esquema sicat)
-- Fuente: docs/modelo-datos.md
-- Criterio de nulabilidad: NOT NULL en PK, FK no marcadas NULL, nombres, booleanos y campos de control.
-- Los atributos descriptivos quedan opcionales porque los datos vienen de planillas Excel incompletas.

-- Catálogos simples -------------------------------------------------------------

CREATE TABLE sicat.tipo_oferente (
    id_tipo_oferente bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre           text    NOT NULL UNIQUE,
    activo           boolean NOT NULL DEFAULT true
);

CREATE TABLE sicat.tipo_canal (
    id_tipo_canal bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre        text    NOT NULL UNIQUE,
    activo        boolean NOT NULL DEFAULT true
);

CREATE TABLE sicat.tipo_lugar (
    id_tipo_lugar bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre        text    NOT NULL UNIQUE,
    activo        boolean NOT NULL DEFAULT true
);

CREATE TABLE sicat.localidad (
    id_localidad bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre       text    NOT NULL,
    provincia    text    NOT NULL,
    activo       boolean NOT NULL DEFAULT true,
    CONSTRAINT uk_localidad_nombre_provincia UNIQUE (nombre, provincia)
);

CREATE TABLE sicat.distincion (
    id_distincion       bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre              text    NOT NULL,
    organismo_otorgante text,
    activo              boolean NOT NULL DEFAULT true
);

-- Clasificación ---------------------------------------------------------------

CREATE TABLE sicat.categoria (
    id_categoria        bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_categoria_padre  bigint REFERENCES sicat.categoria (id_categoria),
    nombre              text    NOT NULL,
    activo              boolean NOT NULL DEFAULT true
);
CREATE INDEX ix_categoria_padre ON sicat.categoria (id_categoria_padre);

CREATE TABLE sicat.caracteristica (
    id_caracteristica bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    grupo             text    NOT NULL,
    nombre            text    NOT NULL,
    admite_cantidad   boolean NOT NULL DEFAULT false,
    activo            boolean NOT NULL DEFAULT true,
    CONSTRAINT uk_caracteristica_grupo_nombre UNIQUE (grupo, nombre)
);

CREATE TABLE sicat.categoria_caracteristica (
    id_categoria      bigint NOT NULL REFERENCES sicat.categoria (id_categoria),
    id_caracteristica bigint NOT NULL REFERENCES sicat.caracteristica (id_caracteristica),
    PRIMARY KEY (id_categoria, id_caracteristica)
);
CREATE INDEX ix_categoria_caracteristica_car ON sicat.categoria_caracteristica (id_caracteristica);

-- Lugar -----------------------------------------------------------------------

CREATE TABLE sicat.lugar (
    id_lugar       bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_lugar_padre bigint REFERENCES sicat.lugar (id_lugar),
    id_tipo_lugar  bigint REFERENCES sicat.tipo_lugar (id_tipo_lugar),
    id_localidad   bigint REFERENCES sicat.localidad (id_localidad),
    nombre         text NOT NULL,
    calle          text,
    altura         integer,
    km             numeric(6, 1),
    calle_cruce    text,
    complemento    varchar(100),
    punto          public.geography(Point, 4326),
    geometria      public.geography(Geometry, 4326),
    estado_geo     text,
    CONSTRAINT ck_lugar_padre_distinto CHECK (id_lugar_padre IS NULL OR id_lugar_padre <> id_lugar)
);
CREATE INDEX ix_lugar_padre     ON sicat.lugar (id_lugar_padre);
CREATE INDEX ix_lugar_tipo      ON sicat.lugar (id_tipo_lugar);
CREATE INDEX ix_lugar_localidad ON sicat.lugar (id_localidad);
CREATE INDEX gx_lugar_punto     ON sicat.lugar USING gist (punto);
CREATE INDEX gx_lugar_geometria ON sicat.lugar USING gist (geometria);
