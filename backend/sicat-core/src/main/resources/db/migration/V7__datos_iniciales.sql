-- SICAT SF · V7 · Datos iniciales
-- Solo roles. Los catálogos se cargan con la importación desde Excel.
-- El primer usuario ADMIN se crea al implementar el módulo de seguridad (paso 3).

INSERT INTO seguridad.rol (nombre, descripcion) VALUES
    ('ADMIN',    'Todo, incluida gestión de usuarios y auditoría'),
    ('CARGADOR', 'ABM de oferentes, establecimientos, ofertas, lugares, contactos y especializaciones; puede publicar'),
    ('CONSULTA', 'Solo lectura en el panel admin'),
    ('OFERENTE', 'Etapa 2: solo sus propios establecimientos (no implementado)');
