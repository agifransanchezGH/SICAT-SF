-- SICAT SF · V4 · Oferta, especializaciones, visitas, horarios y circuitos (esquema sicat)
-- Fuente: docs/modelo-datos.md

CREATE TABLE sicat.oferta (
    id_oferta          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_establecimiento bigint  NOT NULL REFERENCES sicat.establecimiento (id_establecimiento),
    id_categoria       bigint  NOT NULL REFERENCES sicat.categoria (id_categoria),
    acceso             text,
    publico_admitido   text,
    frecuencia         text,
    fecha_relevamiento date,
    activo             boolean NOT NULL DEFAULT true
);
CREATE INDEX ix_oferta_establecimiento ON sicat.oferta (id_establecimiento);
CREATE INDEX ix_oferta_categoria       ON sicat.oferta (id_categoria);

-- Categorías adicionales de una oferta
CREATE TABLE sicat.oferta_categoria (
    id_oferta    bigint NOT NULL REFERENCES sicat.oferta (id_oferta),
    id_categoria bigint NOT NULL REFERENCES sicat.categoria (id_categoria),
    PRIMARY KEY (id_oferta, id_categoria)
);
CREATE INDEX ix_oferta_categoria_cat ON sicat.oferta_categoria (id_categoria);

-- Alojamiento: 1 a 1 con oferta
CREATE TABLE sicat.alojamiento (
    id_oferta              bigint PRIMARY KEY REFERENCES sicat.oferta (id_oferta),
    estrellas              smallint,
    cant_habitaciones      integer,
    total_plazas           integer,
    hab_accesibles         integer,
    plazas_accesibles      integer,
    personal_permanente    integer,
    personal_no_permanente integer,
    CONSTRAINT ck_alojamiento_estrellas CHECK (estrellas IS NULL OR estrellas BETWEEN 1 AND 5)
);

-- Salón: N por oferta
CREATE TABLE sicat.salon (
    id_salon              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_oferta             bigint  NOT NULL REFERENCES sicat.oferta (id_oferta),
    nombre                text    NOT NULL,
    cap_auditorio         integer,
    cap_banquete          integer,
    cap_coctel            integer,
    m2                    numeric,
    estado_conservacion   text,
    estado_funcionamiento text,
    activo                boolean NOT NULL DEFAULT true
);
CREATE INDEX ix_salon_oferta ON sicat.salon (id_oferta);

CREATE TABLE sicat.salon_caracteristica (
    id_salon          bigint NOT NULL REFERENCES sicat.salon (id_salon),
    id_caracteristica bigint NOT NULL REFERENCES sicat.caracteristica (id_caracteristica),
    cantidad          integer,
    PRIMARY KEY (id_salon, id_caracteristica)
);
CREATE INDEX ix_salon_caracteristica_car ON sicat.salon_caracteristica (id_caracteristica);

-- Visita guiada: N por oferta
CREATE TABLE sicat.visita_guiada (
    id_visita        bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_oferta        bigint  NOT NULL REFERENCES sicat.oferta (id_oferta),
    modalidad        text,
    requiere_reserva boolean NOT NULL DEFAULT false,
    habilitada       boolean NOT NULL DEFAULT true
);
CREATE INDEX ix_visita_guiada_oferta ON sicat.visita_guiada (id_oferta);

-- Horario de una oferta (o de una visita puntual de esa oferta)
CREATE TABLE sicat.horario (
    id_horario  bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_oferta   bigint   NOT NULL REFERENCES sicat.oferta (id_oferta),
    id_visita   bigint   REFERENCES sicat.visita_guiada (id_visita),
    dia_semana  smallint NOT NULL,
    hora_desde  time     NOT NULL,
    hora_hasta  time     NOT NULL,
    CONSTRAINT ck_horario_dia_semana CHECK (dia_semana BETWEEN 1 AND 7)
);
CREATE INDEX ix_horario_oferta ON sicat.horario (id_oferta);
CREATE INDEX ix_horario_visita ON sicat.horario (id_visita);

-- Circuito: 1 a 1 con oferta
CREATE TABLE sicat.circuito (
    id_oferta        bigint PRIMARY KEY REFERENCES sicat.oferta (id_oferta),
    id_lugar_partida bigint REFERENCES sicat.lugar (id_lugar),
    duracion_min     integer,
    trazado          public.geography(LineString, 4326)
);
CREATE INDEX ix_circuito_lugar_partida ON sicat.circuito (id_lugar_partida);
CREATE INDEX gx_circuito_trazado       ON sicat.circuito USING gist (trazado);

CREATE TABLE sicat.circuito_parada (
    id_oferta          bigint   NOT NULL REFERENCES sicat.circuito (id_oferta),
    orden              smallint NOT NULL,
    id_establecimiento bigint   NOT NULL REFERENCES sicat.establecimiento (id_establecimiento),
    PRIMARY KEY (id_oferta, orden)
);
CREATE INDEX ix_circuito_parada_establecimiento ON sicat.circuito_parada (id_establecimiento);
