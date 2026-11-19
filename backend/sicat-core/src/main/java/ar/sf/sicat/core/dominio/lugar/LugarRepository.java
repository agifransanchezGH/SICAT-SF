package ar.sf.sicat.core.dominio.lugar;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LugarRepository extends JpaRepository<Lugar, Long>, JpaSpecificationExecutor<Lugar> {

    List<Lugar> findByLugarPadreIdOrderByNombre(Long idLugarPadre);

    boolean existsByLugarPadreId(Long idLugarPadre);

    /** Lugares en alguno de los estados de geocodificación, con id mayor al dado (para recorrer por tandas). */
    List<Lugar> findByEstadoGeoInAndIdGreaterThanOrderById(java.util.Collection<String> estadosGeo, Long id,
                                                           org.springframework.data.domain.Pageable pagina);

    long countByEstadoGeoIn(java.util.Collection<String> estadosGeo);

    long countByEstadoGeo(String estadoGeo);

    /**
     * Para cada lugar pedido, cuántos otros lugares tienen el mismo punto (a menos de un metro). Muchos puntos
     * repetidos son la calle o la zona que devolvió OpenStreetMap, no la puerta. Filas: [id_lugar, cantidad].
     */
    @Query(value = """
            SELECT l.id_lugar,
                   (SELECT count(*) FROM sicat.lugar o
                    WHERE o.id_lugar <> l.id_lugar AND o.punto IS NOT NULL
                      AND public.ST_DWithin(o.punto, l.punto, 1))
            FROM sicat.lugar l
            WHERE l.id_lugar IN (:ids) AND l.punto IS NOT NULL""", nativeQuery = true)
    List<Object[]> puntosCompartidos(@Param("ids") java.util.Collection<Long> ids);

    /** Ids de lugares cuyo punto cae dentro del rectángulo (PostGIS). */
    @Query(value = """
            SELECT l.id_lugar FROM sicat.lugar l
            WHERE l.punto IS NOT NULL
              AND public.ST_Intersects(l.punto,
                    public.ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326)::public.geography)""",
            nativeQuery = true)
    List<Long> idsEnBbox(@Param("minLng") double minLng, @Param("minLat") double minLat,
                         @Param("maxLng") double maxLng, @Param("maxLat") double maxLat);
}
