package ar.sf.sicat.admin.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;

import ar.sf.sicat.admin.PruebaAbm;
import ar.sf.sicat.core.dominio.clasificacion.CaracteristicaRepository;
import ar.sf.sicat.core.dominio.clasificacion.CategoriaRepository;

/** ADR 0017: fusionar, separar y pasar a notas internas características del catálogo. */
class LimpiezaCaracteristicasTest extends PruebaAbm {

    @Autowired
    CategoriaRepository categorias;
    @Autowired
    CaracteristicaRepository caracteristicas;
    @Autowired
    TransactionTemplate tx;

    private long caracteristica(String grupo, String nombre, boolean admiteCantidad) throws Exception {
        return id(post_("/catalogos/caracteristica", "{\"nombre\":\"%s\",\"grupo\":\"%s\",\"admiteCantidad\":%s}"
                .formatted(nombre, grupo, admiteCantidad), 201));
    }

    private void asignar(long establecimiento, String cuerpo) throws Exception {
        put_("/establecimientos/" + establecimiento + "/caracteristicas", cuerpo, 200);
    }

    private List<Integer> asignadas(long establecimiento) throws Exception {
        return leer(get_("/establecimientos/" + establecimiento + "/caracteristicas", 200), "$[*].caracteristicaId");
    }

    @Test
    void fusionaSinDuplicarYConservaLaCantidad() throws Exception {
        String grupo = unico("Baños");
        long monocomando = caracteristica(grupo, "Grifo monocomando", true);
        long monomando = caracteristica(grupo, "Grifo monomando", true);
        long otra = caracteristica(grupo, "Barral", false);
        long oferente = crearOferente();
        long soloVieja = crearEstablecimiento(oferente, null);
        long ambas = crearEstablecimiento(oferente, null);
        asignar(soloVieja, "[{\"caracteristicaId\":%d,\"cantidad\":2},{\"caracteristicaId\":%d}]".formatted(monomando, otra));
        asignar(ambas, "[{\"caracteristicaId\":%d},{\"caracteristicaId\":%d,\"cantidad\":3}]".formatted(monocomando, monomando));
        // una categoría que tenía la absorbida pasa a tener la que queda
        long categoria = crearCategoria(null);
        tx.executeWithoutResult(t -> categorias.findById(categoria).orElseThrow().getCaracteristicas()
                .add(caracteristicas.findById(monomando).orElseThrow()));

        String r = post_("/caracteristicas/fusiones", """
                {"conservarId":%d,"absorberIds":[%d],"nombre":"Grifo monocomando (palanca)"}
                """.formatted(monocomando, monomando), 200);
        assertThat((Integer) leer(r, "$.establecimientos")).isEqualTo(1); // solo se movió la de soloVieja
        assertThat((Integer) leer(r, "$.categorias")).isEqualTo(1);
        assertThat((Integer) leer(r, "$.eliminadas")).isEqualTo(1);
        assertThat((String) leer(r, "$.resultantes[0].nombre")).isEqualTo("Grifo monocomando (palanca)");
        assertThat((Integer) leer(r, "$.resultantes[0].establecimientos")).isEqualTo(2);
        assertThat((Integer) leer(r, "$.resultantes[0].categorias")).isEqualTo(1);

        assertThat(asignadas(soloVieja)).containsExactlyInAnyOrder((int) monocomando, (int) otra);
        assertThat(asignadas(ambas)).containsExactly((int) monocomando); // sin duplicar
        // la conservada no tenía cantidad en "ambas": se toma la de la absorbida
        assertThat((Integer) leer(get_("/establecimientos/" + ambas + "/caracteristicas", 200), "$[0].cantidad")).isEqualTo(3);
        List<Integer> cantidades = leer(get_("/establecimientos/" + soloVieja + "/caracteristicas", 200),
                "$[?(@.caracteristicaId == %d)].cantidad".formatted(monocomando));
        assertThat(cantidades).containsExactly(2); // la cantidad viaja con la asignación movida
        assertThat(caracteristicas.findById(monomando)).isEmpty();

        // queda auditado: la absorbida tiene su ELIMINACION y el uso por establecimiento, su ALTA
        assertThat((List<String>) leer(get_("/auditoria/caracteristica/" + monomando, 200), "$[*].accion"))
                .contains("ELIMINACION");
    }

    @Test
    void validacionesDeLaFusion() throws Exception {
        String grupo = unico("Seguridad");
        long a = caracteristica(grupo, "Sistema contra incendio", false);
        long b = caracteristica(grupo, "Sistema contra incendios", false);
        long deOtroGrupo = caracteristica(unico("Servicios"), "Sistema contra incendio", false);
        post_("/caracteristicas/fusiones", "{\"conservarId\":%d,\"absorberIds\":[]}".formatted(a), 400);
        post_("/caracteristicas/fusiones", "{\"conservarId\":%d,\"absorberIds\":[%d,%d]}".formatted(a, b, b), 400);
        post_("/caracteristicas/fusiones", "{\"conservarId\":%d,\"absorberIds\":[%d]}".formatted(a, a), 400);
        post_("/caracteristicas/fusiones", "{\"conservarId\":%d,\"absorberIds\":[999999]}".formatted(a), 404);
        post_("/caracteristicas/fusiones", "{\"conservarId\":%d,\"absorberIds\":[%d]}".formatted(a, deOtroGrupo), 422);
        long c = caracteristica(grupo, "Matafuegos", false);
        // el nombre nuevo ya lo usa otra del grupo que no está en la fusión
        post_("/caracteristicas/fusiones", "{\"conservarId\":%d,\"absorberIds\":[%d],\"nombre\":\"matafuegos\"}"
                .formatted(a, b), 409);
        assertThat(c).isPositive();
        // solo ADMIN
        pedir(post("/api/admin/v1/caracteristicas/fusiones").contentType(MediaType.APPLICATION_JSON)
                .content("{\"conservarId\":%d,\"absorberIds\":[%d]}".formatted(a, b)), bearerDe("CARGADOR"), 403);
    }

    @Test
    void separaUnaEntradaPegada() throws Exception {
        String grupo = unico("Servicios");
        long pegada = caracteristica(grupo, "Servicio medico.Estacionamiento", false);
        long estacionamiento = caracteristica(grupo, "Estacionamiento", false);
        long oferente = crearOferente();
        long est = crearEstablecimiento(oferente, null);
        long yaTenia = crearEstablecimiento(oferente, null);
        asignar(est, "[{\"caracteristicaId\":%d}]".formatted(pegada));
        asignar(yaTenia, "[{\"caracteristicaId\":%d},{\"caracteristicaId\":%d}]".formatted(pegada, estacionamiento));

        String r = post_("/caracteristicas/" + pegada + "/separacion",
                "{\"nombres\":[\"Servicio médico\",\"estacionamiento\"]}", 200);
        List<Integer> partes = leer(r, "$.resultantes[*].id");
        assertThat(partes).hasSize(2).contains((int) estacionamiento); // reutiliza la existente
        long medico = partes.stream().filter(p -> p != estacionamiento).findFirst().orElseThrow();
        assertThat(asignadas(est)).containsExactlyInAnyOrder((int) medico, (int) estacionamiento);
        assertThat(asignadas(yaTenia)).containsExactlyInAnyOrder((int) medico, (int) estacionamiento);
        assertThat(caracteristicas.findById(pegada)).isEmpty();

        post_("/caracteristicas/" + medico + "/separacion", "{\"nombres\":[\"Solo una\"]}", 400);
        post_("/caracteristicas/999999/separacion", "{\"nombres\":[\"A\",\"B\"]}", 404);
    }

    @Test
    void pasaUnaDireccionANotasInternas() throws Exception {
        String grupo = unico("Accesibilidad: ingreso");
        long direccion = caracteristica(grupo, "Por calle Lavaisse 610", false);
        long rampa = caracteristica(grupo, "Rampas", false);
        long est = id(post_("/establecimientos", "{\"oferenteId\":%d,\"nombre\":\"%s\",\"notasInternas\":\"Llamar antes\"}"
                .formatted(crearOferente(), unico("Club")), 201));
        asignar(est, "[{\"caracteristicaId\":%d},{\"caracteristicaId\":%d}]".formatted(direccion, rampa));

        String r = post_("/caracteristicas/" + direccion + "/a-notas-internas", "", 200);
        assertThat((Integer) leer(r, "$.establecimientos")).isEqualTo(1);
        assertThat(asignadas(est)).containsExactly((int) rampa);
        assertThat((String) leer(get_("/establecimientos/" + est, 200), "$.notasInternas"))
                .isEqualTo("Llamar antes\n\n" + grupo + ": Por calle Lavaisse 610");

        // si la usa una categoría (que no tiene notas), no se puede
        long enCategoria = caracteristica(grupo, "Calle 9 de Julio 2239", false);
        long categoria = crearCategoria(null);
        tx.executeWithoutResult(t -> categorias.findById(categoria).orElseThrow().getCaracteristicas()
                .add(caracteristicas.findById(enCategoria).orElseThrow()));
        post_("/caracteristicas/" + enCategoria + "/a-notas-internas", "", 409);
    }

    @Test
    void usoPorGrupo() throws Exception {
        String grupo = "Pagos-" + java.util.UUID.randomUUID().toString().substring(0, 8); // sin espacios: MockMvc recodifica %20
        long efectivo = caracteristica(grupo, "Efectivo", false);
        caracteristica(grupo, "Billetera virtual", false);
        asignar(crearEstablecimiento(crearOferente(), null), "[{\"caracteristicaId\":%d}]".formatted(efectivo));
        String lista = get_("/caracteristicas/uso?grupo=" + grupo, 200);
        assertThat((List<String>) leer(lista, "$[*].nombre")).containsExactly("Billetera virtual", "Efectivo");
        assertThat((Integer) leer(lista, "$[1].establecimientos")).isEqualTo(1);
    }
}
