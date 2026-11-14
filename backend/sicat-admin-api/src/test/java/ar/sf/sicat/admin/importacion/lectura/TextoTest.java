package ar.sf.sicat.admin.importacion.lectura;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextoTest {

    @Test
    void valoresVaciosYNumerosDeExcel() {
        assertThat(Texto.limpio(" - ")).isNull();
        assertThat(Texto.limpio("S/N")).isNull();
        assertThat(Texto.limpio("  Hola   mundo ")).isEqualTo("Hola mundo");
        assertThat(Texto.numeroComoTexto("3.425260204E9")).isEqualTo("3425260204");
        assertThat(Texto.numeroComoTexto("4564143.0")).isEqualTo("4564143");
        assertThat(Texto.clave("Café Ñandú  S.A.")).isEqualTo("cafe nandu s a");
    }

    @Test
    void listasRespetanParentesisYAbreviaturas() {
        assertThat(Texto.lista("Información turística, Info en formatos (PDF, Código QR), Personal bilingüe"))
                .containsExactly("Información turística", "Info en formatos (PDF, Código QR)", "Personal bilingüe");
        assertThat(Texto.lista("Informacion turistica. Servicio medico. Admite contingentes."))
                .containsExactly("Informacion turistica", "Servicio medico", "Admite contingentes");
        assertThat(Texto.lista("Pasillos amplios (min. 1,50 m). Sillas cómodas"))
                .containsExactly("Pasillos amplios (min. 1,50 m)", "Sillas cómodas");
    }

    @Test
    void contactosYTelefonos() {
        assertThat(Texto.valoresContacto("a@b.com / c@d.com")).containsExactly("a@b.com", "c@d.com");
        assertThat(Texto.valoresContacto("https://www.cinemark.com.ar/")).containsExactly("https://www.cinemark.com.ar/");
        assertThat(Texto.valoresTelefono("0342-6103638/4506600")).containsExactly("0342-6103638", "4506600");
        assertThat(Texto.valoresTelefono("3.424309173E9")).containsExactly("3424309173");
        assertThat(Texto.usuarioInstagram("https://www.instagram.com/stl_operador/")).isEqualTo("stl_operador");
        assertThat(Texto.usuarioInstagram("@museodelaconstitucion")).isEqualTo("museodelaconstitucion");
        assertThat(Texto.usuarioInstagram("Tourfe (@tourfeturismo) · Santa Fe/tourfeturismo/")).isEqualTo("tourfeturismo");
    }

    @Test
    void direccionesYNumeros() {
        assertThat(Texto.calleYAltura("San Martín 2068")).hasValueSatisfying(p -> {
            assertThat(p[0]).isEqualTo("San Martín");
            assertThat(p[1]).isEqualTo("2068");
        });
        assertThat(Texto.calleYAltura("Reg. 12 de Infantería 783.")).hasValueSatisfying(p -> assertThat(p[1]).isEqualTo("783"));
        assertThat(Texto.calleYAltura("Ruta Nac. 168 km 80")).isEmpty();
        assertThat(Texto.repararFecha("4 de January 0895")).isEqualTo("4 de Enero 895");
        assertThat(Texto.repararFecha("San Martín 2068")).isEqualTo("San Martín 2068");
        assertThat(Texto.calleYAltura("Dique I - Puerto Santa Fe")).isEmpty();
        assertThat(Texto.anio("1969 (reformado 2019)")).contains((short) 1969);
        assertThat(Texto.entero("84 - STANDAR Y SUIT")).contains(84);
        assertThat(Texto.afirmativo("Sí*")).isTrue();
        assertThat(Texto.afirmativo("No")).isFalse();
    }
}
