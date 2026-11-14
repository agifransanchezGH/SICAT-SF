-- Datos de ejemplo para los tests de la API Pública (se cargan como sicat_admin_rw).
-- Ids fijos >= 100. Casos: publicado completo, borrador, oculto, oferente inactivo, categoría inactiva,
-- visita no habilitada, salón inactivo, contacto no público y parada en un establecimiento no publicado.

INSERT INTO sicat.tipo_oferente (id_tipo_oferente, nombre) OVERRIDING SYSTEM VALUE VALUES (100, 'Privado');
INSERT INTO sicat.oferente (id_oferente, id_tipo_oferente, nombre, activo) OVERRIDING SYSTEM VALUE VALUES
    (100, 100, 'Hotelera del Litoral', true),
    (101, 100, 'Oferente Inactivo', false);

INSERT INTO sicat.tipo_canal (id_tipo_canal, nombre) OVERRIDING SYSTEM VALUE VALUES (100, 'Instagram'), (101, 'Teléfono');

INSERT INTO sicat.localidad (id_localidad, nombre, provincia) OVERRIDING SYSTEM VALUE VALUES
    (100, 'Santa Fe', 'Santa Fe'),
    (101, 'Santo Tomé', 'Santa Fe');

INSERT INTO sicat.lugar (id_lugar, nombre, calle, altura, id_localidad, punto) OVERRIDING SYSTEM VALUE VALUES
    (100, 'Costanera', 'Av. Costanera', 1500, 100, public.ST_GeogFromText('SRID=4326;POINT(-60.700 -31.630)')),
    (101, 'Centro', 'San Martín', 2000, 100, public.ST_GeogFromText('SRID=4326;POINT(-60.705 -31.645)')),
    (102, 'Santo Tomé centro', 'Libertad', 100, 101, public.ST_GeogFromText('SRID=4326;POINT(-60.765 -31.665)')),
    (103, 'Plaza 25 de Mayo', NULL, NULL, 100, public.ST_GeogFromText('SRID=4326;POINT(-60.710 -31.650)'));

INSERT INTO sicat.categoria (id_categoria, id_categoria_padre, nombre, activo) OVERRIDING SYSTEM VALUE VALUES
    (100, NULL, 'Gastronomía', true),
    (101, 100, 'Parrilla', true),
    (102, NULL, 'Alojamiento', true),
    (103, 102, 'Hotel', true),
    (104, NULL, 'Salones de eventos', true),
    (105, NULL, 'Categoría inactiva', false),
    (106, NULL, 'Circuitos', true);

INSERT INTO sicat.establecimiento
    (id_establecimiento, id_oferente, id_lugar, nombre, descripcion, estado_funcionamiento, motivo_estado,
     fecha_reapertura, estado_publicacion, destacado) OVERRIDING SYSTEM VALUE VALUES
    (100, 100, 100, 'Hotel Ñandú', 'Frente al río', 'Abierto', NULL, NULL, 'PUBLICADO', true),
    (101, 100, 101, 'Parrilla Don Café', NULL, 'Cerrado temporalmente', 'Refacción', '2027-03-01', 'PUBLICADO', false),
    (102, 100, 100, 'Borrador SRL', NULL, NULL, NULL, NULL, 'BORRADOR', false),
    (103, 100, 100, 'Oculto SA', NULL, NULL, NULL, NULL, 'OCULTO', false),
    (104, 101, 100, 'De oferente inactivo', NULL, NULL, NULL, NULL, 'PUBLICADO', false),
    (105, 100, 102, 'Bodegón Santo Tomé', NULL, NULL, NULL, NULL, 'PUBLICADO', false),
    (106, 100, 103, 'Recorrido Histórico', NULL, NULL, NULL, NULL, 'PUBLICADO', false);

INSERT INTO sicat.oferta (id_oferta, id_establecimiento, id_categoria, acceso, activo) OVERRIDING SYSTEM VALUE VALUES
    (100, 100, 103, 'Con reserva', true),
    (101, 101, 101, 'Libre', true),
    (102, 101, 105, NULL, true),
    (103, 105, 100, NULL, true),
    (104, 106, 106, 'Gratuito', true),
    (105, 102, 101, NULL, true);

INSERT INTO sicat.oferta_categoria (id_oferta, id_categoria) VALUES (100, 104);

INSERT INTO sicat.alojamiento (id_oferta, estrellas, cant_habitaciones, total_plazas, personal_permanente)
VALUES (100, 4, 40, 90, 10);

INSERT INTO sicat.salon (id_salon, id_oferta, nombre, cap_auditorio, estado_conservacion, activo) OVERRIDING SYSTEM VALUE VALUES
    (100, 100, 'Salón Río', 200, 'Bueno', true),
    (101, 100, 'Salón cerrado', 50, NULL, false);

INSERT INTO sicat.visita_guiada (id_visita, id_oferta, modalidad, requiere_reserva, habilitada) OVERRIDING SYSTEM VALUE VALUES
    (100, 104, 'Grupal con guía', true, true),
    (101, 104, 'Nocturna', false, false);

INSERT INTO sicat.horario (id_horario, id_oferta, id_visita, dia_semana, hora_desde, hora_hasta) OVERRIDING SYSTEM VALUE VALUES
    (100, 100, NULL, 1, '08:00', '20:00'),
    (101, 101, NULL, 5, '20:00', '02:00'),
    (102, 104, 100, 6, '10:00', '11:30'),
    (103, 104, 101, 6, '21:00', '22:00');

INSERT INTO sicat.circuito (id_oferta, id_lugar_partida, duracion_min, trazado) VALUES
    (104, 103, 90, public.ST_GeogFromText('SRID=4326;LINESTRING(-60.710 -31.650, -60.700 -31.630, -60.705 -31.645)'));

INSERT INTO sicat.circuito_parada (id_oferta, orden, id_establecimiento) VALUES
    (104, 1, 100), (104, 2, 102), (104, 3, 101);

INSERT INTO sicat.canal_contacto (id_establecimiento, id_oferente, id_tipo_canal, valor, uso, es_publico, orden) VALUES
    (100, NULL, 100, '@hotelnandu', 'Reservas', true, 1),
    (100, NULL, 101, '342 000-0000', 'Administración', false, 2),
    (NULL, 100, 101, '342 111-1111', 'Central', true, 3);

INSERT INTO sicat.caracteristica (id_caracteristica, grupo, nombre, admite_cantidad) OVERRIDING SYSTEM VALUE VALUES
    (100, 'Servicios', 'Wi-Fi', false),
    (101, 'Capacidad', 'Habitaciones', true);
INSERT INTO sicat.establecimiento_caracteristica (id_establecimiento, id_caracteristica, cantidad) VALUES
    (100, 100, NULL), (100, 101, 40);

INSERT INTO sicat.distincion (id_distincion, nombre, organismo_otorgante) OVERRIDING SYSTEM VALUE VALUES
    (100, 'Excelencia turística', 'Ministerio de Turismo');
INSERT INTO sicat.establecimiento_distincion (id_establecimiento, id_distincion, anio_otorgamiento) VALUES (100, 100, 2024);
