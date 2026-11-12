-- SICAT SF · V3 · Oferente, establecimiento y contacto (esquema sicat)
-- Fuente: docs/modelo-datos.md

CREATE TABLE sicat.oferente (
    id_oferente      bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_tipo_oferente bigint  NOT NULL REFERENCES sicat.tipo_oferente (id_tipo_oferente),
    nombre           text    NOT NULL,
    activo           boolean NOT NULL DEFAULT true
);
CREATE INDEX ix_oferente_tipo ON sicat.oferente (id_tipo_oferente);

CREATE TABLE sicat.establecimiento (
    id_establecimiento    bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_oferente           bigint  NOT NULL REFERENCES sicat.oferente (id_oferente),
    id_lugar              bigint  REFERENCES sicat.lugar (id_lugar),
    nombre                text    NOT NULL,
    descripcion           text,
    anio_inauguracion     smallint,
    estado_funcionamiento text,
    motivo_estado         text,
    fecha_reapertura      date,
    estado_publicacion    text    NOT NULL DEFAULT 'BORRADOR',
    destacado             boolean NOT NULL DEFAULT false,
    activo                boolean NOT NULL DEFAULT true,
    CONSTRAINT ck_establecimiento_estado_publicacion
        CHECK (estado_publicacion IN ('BORRADOR', 'PUBLICADO', 'OCULTO'))
);
CREATE INDEX ix_establecimiento_oferente ON sicat.establecimiento (id_oferente);
CREATE INDEX ix_establecimiento_lugar    ON sicat.establecimiento (id_lugar);
CREATE INDEX ix_establecimiento_estado   ON sicat.establecimiento (estado_publicacion);

CREATE TABLE sicat.referente (
    id_referente       bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_establecimiento bigint  NOT NULL REFERENCES sicat.establecimiento (id_establecimiento),
    nombre             text    NOT NULL,
    cargo              text,
    telefono           text,
    correo             text,
    activo             boolean NOT NULL DEFAULT true
);
CREATE INDEX ix_referente_establecimiento ON sicat.referente (id_establecimiento);

CREATE TABLE sicat.establecimiento_distincion (
    id_establecimiento bigint NOT NULL REFERENCES sicat.establecimiento (id_establecimiento),
    id_distincion      bigint NOT NULL REFERENCES sicat.distincion (id_distincion),
    anio_otorgamiento  smallint,
    url_verificacion   text,
    PRIMARY KEY (id_establecimiento, id_distincion)
);
CREATE INDEX ix_establecimiento_distincion_dis ON sicat.establecimiento_distincion (id_distincion);

CREATE TABLE sicat.establecimiento_caracteristica (
    id_establecimiento bigint NOT NULL REFERENCES sicat.establecimiento (id_establecimiento),
    id_caracteristica  bigint NOT NULL REFERENCES sicat.caracteristica (id_caracteristica),
    cantidad           integer,
    PRIMARY KEY (id_establecimiento, id_caracteristica)
);
CREATE INDEX ix_establecimiento_caracteristica_car ON sicat.establecimiento_caracteristica (id_caracteristica);

-- Un canal pertenece a un establecimiento o a un oferente (exactamente uno de los dos).
CREATE TABLE sicat.canal_contacto (
    id_canal           bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_establecimiento bigint  REFERENCES sicat.establecimiento (id_establecimiento),
    id_oferente        bigint  REFERENCES sicat.oferente (id_oferente),
    id_tipo_canal      bigint  NOT NULL REFERENCES sicat.tipo_canal (id_tipo_canal),
    valor              text    NOT NULL,
    uso                text,
    es_publico         boolean NOT NULL DEFAULT false,
    orden              smallint,
    CONSTRAINT ck_canal_contacto_duenio CHECK (num_nonnulls(id_establecimiento, id_oferente) = 1)
);
CREATE INDEX ix_canal_contacto_establecimiento ON sicat.canal_contacto (id_establecimiento);
CREATE INDEX ix_canal_contacto_oferente        ON sicat.canal_contacto (id_oferente);
CREATE INDEX ix_canal_contacto_tipo            ON sicat.canal_contacto (id_tipo_canal);
