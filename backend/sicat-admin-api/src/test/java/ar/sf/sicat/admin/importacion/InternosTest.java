package ar.sf.sicat.admin.importacion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ar.sf.sicat.admin.importacion.modelo.Registro;

/** Criterio de ADR 0015: qué va a notas internas y qué grupos de características son internos. */
class InternosTest {

    @Test
    void lasPreguntasDeLaEncuestaVanANotasInternas() {
        Registro r = new Registro("Hoja", 2);
        r.agregarDescripcion(null, "Hotel frente al río.");
        r.agregarDescripcion("Líneas de colectivo cercanas", "C, 3, 10");
        r.agregarDescripcion("Si seleccionó SI, detallar cantidad de ascensores", "3");
        r.agregarDescripcion("Seleccione lo que realiza/tiene el alojamiento", "Encuesta de satisfacción");
        r.agregarDescripcion("Sí marcó otros, detallar", "Cochera");
        r.agregarDescripcion("Cantidad de personal efectivo/permanente", "12");
        r.agregarDescripcion("OBSERVACIONES", "Relevado en marzo");
        r.agregarDescripcion("INFORMACIÓN ADICIONAL / OBSERVACIONES", "Abre feriados");

        assertThat(r.descripcion).isEqualTo("""
                Hotel frente al río.

                Líneas de colectivo cercanas: C, 3, 10

                INFORMACIÓN ADICIONAL / OBSERVACIONES: Abre feriados""");
        assertThat(r.notasInternas)
                .contains("ascensores: 3", "Encuesta de satisfacción", "Cochera", "permanente: 12", "OBSERVACIONES: Relevado")
                .doesNotContain("colectivo", "Abre feriados");
    }

    @Test
    void gruposInternos() {
        assertThat(Internos.grupoInterno("Plataformas de venta")).isTrue();
        assertThat(Internos.grupoInterno("SEGURIDAD")).isTrue();
        assertThat(Internos.grupoInterno("Informacion y calidad")).isTrue();
        assertThat(Internos.grupoInterno("Sistema de reserva")).isFalse();
        assertThat(Internos.grupoInterno("Servicios")).isFalse();
        assertThat(Internos.grupoInterno(null)).isFalse();
    }
}
