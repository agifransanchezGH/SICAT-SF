package ar.sf.sicat.admin.importacion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ar.sf.sicat.admin.importacion.modelo.Registro;

/** Celdas de categoría con texto descriptivo pegado (caso ABRAXAS Centro Cultural). */
class SubcategoriaTest {

    @Test
    void textoLargoConComaSeSeparaEnSubcategoriaYDescripcion() {
        Registro r = new Registro("Salones", 10);
        Interprete.fijarSubcategoria(r, "Espacio privado particular, SE REALIZAN DIFERENTES TALLERES ARTÍSTICOS . "
                + "SE ALQUILA ESPACIO PARA EVENTOS, REUNIONES, EXPOSICIONES.");
        assertThat(r.subcategoria).isEqualTo("Espacio privado particular");
        assertThat(r.descripcion).startsWith("SE REALIZAN DIFERENTES TALLERES").endsWith("EXPOSICIONES.");
    }

    @Test
    void categoriaCortaConComaNoSeToca() {
        Registro r = new Registro("Salones", 11);
        Interprete.fijarSubcategoria(r, "Entidad Intermedia, Club");
        assertThat(r.subcategoria).isEqualTo("Entidad Intermedia, Club");
        assertThat(r.descripcion).isNull();
    }

    @Test
    void enMayusculasSeCapitalizaComoAntes() {
        Registro r = new Registro("Gastronomía", 2);
        Interprete.fijarSubcategoria(r, "BARES Y CAFETERIAS");
        assertThat(r.subcategoria).isEqualTo("Bares y cafeterias");
        Interprete.fijarSubcategoria(r, null);
        assertThat(r.subcategoria).isNull();
    }
}
