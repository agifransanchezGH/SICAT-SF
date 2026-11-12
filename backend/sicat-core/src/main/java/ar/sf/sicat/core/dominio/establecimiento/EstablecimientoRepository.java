package ar.sf.sicat.core.dominio.establecimiento;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EstablecimientoRepository
        extends JpaRepository<Establecimiento, Long>, JpaSpecificationExecutor<Establecimiento> {

    List<Establecimiento> findByOferenteId(Long idOferente);

    /** Establecimientos con lugar a menos de {@code radioMetros} del punto (PostGIS, sobre geography). */
    @Query(value = """
            SELECT e.* FROM sicat.establecimiento e
            JOIN sicat.lugar l ON l.id_lugar = e.id_lugar
            WHERE e.activo
              AND public.ST_DWithin(l.punto,
                    public.ST_SetSRID(public.ST_MakePoint(:lng, :lat), 4326)::public.geography,
                    :radioMetros)
            ORDER BY public.ST_Distance(l.punto,
                    public.ST_SetSRID(public.ST_MakePoint(:lng, :lat), 4326)::public.geography)
            """, nativeQuery = true)
    List<Establecimiento> buscarCercanos(@Param("lat") double lat,
                                         @Param("lng") double lng,
                                         @Param("radioMetros") double radioMetros);
}
