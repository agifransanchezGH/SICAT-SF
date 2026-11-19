package ar.sf.sicat.admin.publicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ar.sf.sicat.admin.PruebaAbm;

/**
 * Listado de establecimientos: requisitos faltantes por fila y filtro "listos". El filtro es SQL y los
 * faltantes salen de PublicacionService.evaluar: este test comprueba que las dos versiones de la regla coinciden.
 */
class ListosParaPublicarTest extends PruebaAbm {

    private String buscar(long id, String extra) throws Exception {
        String nombre = leer(get_("/establecimientos/" + id, 200), "$.nombre");
        String sufijoUnico = nombre.substring(nombre.lastIndexOf(' ') + 1); // sin espacios: nada que codificar
        return get_("/establecimientos?incluirInactivos=true" + extra + "&q=" + sufijoUnico, 200);
    }

    private boolean estaEnListos(long id) throws Exception {
        List<Integer> ids = leer(buscar(id, "&listos=true"), "$.contenido[*].id");
        return ids.contains((int) id);
    }

    private List<String> codigos(long id) throws Exception {
        return leer(buscar(id, ""), "$.contenido[0].faltantes[*].codigo");
    }

    @Test
    void faltantesPorFilaYFiltroListosCoinciden() throws Exception {
        long oferente = crearOferente();
        long categoria = crearCategoria(null);

        long completo = crearEstablecimiento(oferente, crearLugar(-31.63, -60.70, null));
        crearOferta(completo, categoria);

        long sinNada = crearEstablecimiento(oferente, null);

        long sinCoordenadas = crearEstablecimiento(oferente, id(post_("/lugares", "{\"nombre\":\"Sin punto\"}", 201)));
        crearOferta(sinCoordenadas, categoria);

        long automatico = crearEstablecimiento(oferente, id(post_("/lugares", """
                {"nombre":"%s","coordenadas":{"lat":-31.6,"lng":-60.7},"estadoGeo":"AUTOMATICO"}
                """.formatted(unico("Auto")), 201)));
        crearOferta(automatico, categoria);

        long categoriaQueSeDaDeBaja = crearCategoria(null);
        long conCategoriaInactiva = crearEstablecimiento(oferente, crearLugar(-31.62, -60.71, null));
        crearOferta(conCategoriaInactiva, categoriaQueSeDaDeBaja);
        delete_("/categorias/" + categoriaQueSeDaDeBaja, 204);

        long publicado = crearEstablecimiento(oferente, crearLugar(-31.61, -60.72, null));
        crearOferta(publicado, categoria);
        post_("/establecimientos/" + publicado + "/publicar", "", 200);

        long oculto = crearEstablecimiento(oferente, crearLugar(-31.64, -60.73, null));
        crearOferta(oculto, categoria);
        post_("/establecimientos/" + oculto + "/publicar", "", 200);
        post_("/establecimientos/" + oculto + "/despublicar", "", 200);

        Map<Long, List<String>> esperado = Map.of(
                completo, List.of(),
                sinNada, List.of("LUGAR", "OFERTA_ACTIVA"),
                sinCoordenadas, List.of("COORDENADAS"),
                automatico, List.of("UBICACION_VERIFICADA"),
                conCategoriaInactiva, List.of("OFERTA_ACTIVA"),
                publicado, List.of(),
                oculto, List.of());
        for (var caso : esperado.entrySet()) {
            long id = caso.getKey();
            assertThat(codigos(id)).as("faltantes de %d", id).isEqualTo(caso.getValue());
            String estado = leer(buscar(id, ""), "$.contenido[0].estadoPublicacion");
            boolean listoSegunJava = caso.getValue().isEmpty() && !"PUBLICADO".equals(estado);
            assertThat(estaEnListos(id)).as("listos de %d", id).isEqualTo(listoSegunJava);
        }
        assertThat((String) leer(buscar(automatico, ""), "$.contenido[0].faltantes[0].texto"))
                .isEqualTo("ubicación sin verificar");

        // oferente dado de baja: deja de estar listo
        long otroOferente = crearOferente();
        long deOferenteInactivo = crearEstablecimiento(otroOferente, crearLugar(-31.65, -60.74, null));
        crearOferta(deOferenteInactivo, categoria);
        assertThat(estaEnListos(deOferenteInactivo)).isTrue();
        delete_("/oferentes/" + otroOferente, 204);
        assertThat(codigos(deOferenteInactivo)).contains("OFERENTE_ACTIVO");
        assertThat(estaEnListos(deOferenteInactivo)).isFalse();
    }
}
