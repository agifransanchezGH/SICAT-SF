# 0015 · Datos internos de la importación: características y notas

- Estado: Aceptado
- Fecha: 2026-11-14

## Contexto
Al publicar los primeros establecimientos (ADR 0012) se vio que la importación desde Excel (ADR 0010) dejaba en el sitio público datos pensados para la Dirección de Turismo:
- **Descripciones** con las preguntas de la encuesta ("Si seleccionó SI, detallar…", "Seleccione lo que realiza…", personal, remodelaciones): la importación mandaba a la descripción todo lo que no reconocía, como "Pregunta: respuesta". Eran 80 establecimientos activos.
- **Características de uso interno** ("Plataformas de venta", "¿Tiene alguna automatización?", etc.) que se mostraban en la ficha.

## Decisión
- **`caracteristica.es_publica`** (nueva columna, por defecto `true`): la vista `publico.v_caracteristica_publica` solo muestra las públicas. Se edita por ítem en Catálogos ("Se muestra en el sitio"); el editor de características marca las internas.
  - Quedan internos los grupos: Plataformas de venta, Redes que utiliza, Uso de redes sociales, Información y calidad, Dominio, Otros y Seguridad. Sistema de reserva sigue siendo público.
- **`establecimiento.notas_internas`** (nueva columna): texto solo para el panel, ninguna vista pública lo expone. Se edita en la ficha del establecimiento, debajo de la descripción.
- **Migración `V11`**: agrega las columnas, marca esos grupos como internos y separa las descripciones por párrafo: los que empiezan con preguntas de la encuesta o datos internos pasan a `notas_internas`; el resto queda en la descripción. Patrón: "Si seleccionó…", "Seleccione/Seleccionar…", "Sí marcó…", remodelaciones/reformas, cantidad de personal, capital humano, organismo otorgante, distinciones de calidad, estado de (las) rampas, observaciones y mail. No se pierde texto: solo cambia de campo.
- **Importación**: aplica el mismo criterio (`Internos.java`). Los párrafos internos van a `notas_internas`, y una característica nueva es interna si su grupo es uno de los internos o si todas las existentes de su grupo lo son.

## Consecuencias
- Cambia el modelo de datos (`docs/modelo-datos.md`): dos columnas nuevas.
- El criterio de párrafos internos vive en dos lugares (la migración, ya aplicada, y `Internos.java`); si se ajusta, cambiar `Internos.java` y, si hace falta, otra migración para lo ya cargado.
- Quedan casos dudosos para revisar a mano (p. ej. "Sí marcó otros, detallar" a veces es información útil). Se corrigen desde el panel moviendo el texto entre descripción y notas internas.
