package ar.sf.sicat.admin.importacion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ar.sf.sicat.admin.importacion.lectura.Texto;
import ar.sf.sicat.admin.importacion.modelo.Registro;

/** ADR 0017: la importación normaliza los nombres de característica y no crea duplicados por tipeo. */
class CaracteristicasImportTest {

    @Test
    void normalizaElNombre() {
        assertThat(Texto.normalizarCaracteristica(" .Darsenas  de estacionamiento   anchas ")).isEqualTo("Darsenas de estacionamiento anchas");
        assertThat(Texto.normalizarCaracteristica("Barandas ( en escaleras )")).isEqualTo("Barandas (en escaleras)");
        assertThat(Texto.normalizarCaracteristica("RAMPAS EN LAS ESQUINAS")).isEqualTo("Rampas en las esquinas");
        assertThat(Texto.normalizarCaracteristica("TV")).isEqualTo("TV");
        assertThat(Texto.normalizarCaracteristica("wifi -")).isEqualTo("Wifi");
        assertThat(Texto.normalizarCaracteristica(" - ")).isNull();
    }

    @Test
    void reconoceLaMismaCaracteristicaConErroresDeTipeo() {
        assertThat(Texto.mismaCaracteristica("Grifo monocomando", "Grifo monomando")).isTrue();
        assertThat(Texto.mismaCaracteristica("Sistema contra incendio", "Sistema contra incendios")).isTrue();
        assertThat(Texto.mismaCaracteristica("Uso de lenguaje claro en la comunicación escrita y oral",
                "Uso de lenguaje claro en la comuniciacion escrita y oral")).isTrue();
        assertThat(Texto.mismaCaracteristica("Señalética", "SENALETICA")).isTrue();
        // distintas de verdad
        assertThat(Texto.mismaCaracteristica("Rampas", "Rampas con asistencia de acceso al espacio")).isFalse();
        assertThat(Texto.mismaCaracteristica("Piscina climatizada", "Piscina no climatizada")).isFalse();
        assertThat(Texto.mismaCaracteristica("Sala para eventos (cap. 200 personas)", "Sala para eventos (cap. 700 personas)")).isFalse();
        assertThat(Texto.mismaCaracteristica("Mesas", "Misas")).isFalse(); // nombres cortos: solo iguales
    }

    @Test
    void reconoceDirecciones() {
        assertThat(Texto.pareceDireccion("Por calle Lavaisse 610")).isTrue();
        assertThat(Texto.pareceDireccion("Posee ingresos (uno por escalera) Av Almirante Brown 5933 y el otro")).isTrue();
        assertThat(Texto.pareceDireccion("Rampas accesibles desde el estacionamiento")).isFalse();
        assertThat(Texto.pareceDireccion("Ciclovía o bicisenda a menos de 200 m")).isFalse();
    }

    @Test
    void elRegistroNoRepiteLaMismaCaracteristica() {
        Registro r = new Registro("Hoja", 2);
        r.agregarCaracteristica("Accesibilidad: baños", "Grifo monocomando", null);
        r.agregarCaracteristica("Accesibilidad: baños", "GRIFO MONOMANDO", null);
        r.agregarCaracteristica("Accesibilidad: baños", ".Barandas", null);
        r.agregarCaracteristica("Servicios", "Grifo monocomando", null); // otro grupo: se mantiene
        assertThat(r.caracteristicas).extracting(Registro.CaracteristicaValor::nombre)
                .containsExactly("Grifo monocomando", "Barandas", "Grifo monocomando");
    }

    @Test
    void lasNotasInternasSeAcumulan() {
        Registro r = new Registro("Hoja", 2);
        r.agregarNotaInterna("Accesibilidad: ingreso: Por calle Lavaisse 610");
        r.agregarNotaInterna("Otra nota");
        assertThat(r.notasInternas).isEqualTo("Accesibilidad: ingreso: Por calle Lavaisse 610\n\nOtra nota");
    }
}
