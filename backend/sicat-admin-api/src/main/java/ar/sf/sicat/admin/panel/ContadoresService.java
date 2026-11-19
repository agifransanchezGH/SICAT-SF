package ar.sf.sicat.admin.panel;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.sf.sicat.admin.importacion.DuplicadosService;
import ar.sf.sicat.admin.panel.dto.ContadoresDto;
import ar.sf.sicat.core.dominio.establecimiento.EstablecimientoRepository;

/**
 * Contadores del menú. Ubicaciones, listos y contactos salen de una sola consulta SQL; los duplicados se
 * cuentan aparte con los nombres (una consulta liviana) porque la normalización del nombre vive en Java
 * ({@code Texto.clave}) y así el número coincide siempre con la pantalla de duplicados.
 */
@Service
public class ContadoresService {

    private final EstablecimientoRepository establecimientos;
    private final DuplicadosService duplicados;

    public ContadoresService(EstablecimientoRepository establecimientos, DuplicadosService duplicados) {
        this.establecimientos = establecimientos;
        this.duplicados = duplicados;
    }

    @Transactional(readOnly = true)
    public ContadoresDto contadores() {
        EstablecimientoRepository.ContadoresPanel c = establecimientos.contadores();
        return new ContadoresDto(c.getUbicacionesARevisar(), c.getListosParaPublicar(), duplicados.contarGrupos(),
                c.getContactosARevisar());
    }
}
