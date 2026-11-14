package ar.sf.sicat.core.vistapublica;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EstablecimientoPublicoRepository
        extends VistaRepository<EstablecimientoPublico, Long>, JpaSpecificationExecutor<EstablecimientoPublico> {

    List<EstablecimientoPublico> findByIdIn(Collection<Long> ids);

    /**
     * Ids y distancia en metros de los establecimientos publicados a menos de {@code radioMetros},
     * del más cercano al más lejano. Cada fila: [id_establecimiento, distancia].
     */
    @Query(value = """
            SELECT ep.id_establecimiento,
                   public.ST_Distance(
                       public.ST_SetSRID(public.ST_MakePoint(ep.lng, ep.lat), 4326)::public.geography,
                       public.ST_SetSRID(public.ST_MakePoint(:lng, :lat), 4326)::public.geography) AS distancia
            FROM publico.v_establecimiento_publico ep
            WHERE ep.lat IS NOT NULL
              AND public.ST_DWithin(
                       public.ST_SetSRID(public.ST_MakePoint(ep.lng, ep.lat), 4326)::public.geography,
                       public.ST_SetSRID(public.ST_MakePoint(:lng, :lat), 4326)::public.geography,
                       :radioMetros)
            ORDER BY distancia
            LIMIT :limite""", nativeQuery = true)
    List<Object[]> cercanos(@Param("lat") double lat, @Param("lng") double lng,
                            @Param("radioMetros") double radioMetros, @Param("limite") int limite);
}
