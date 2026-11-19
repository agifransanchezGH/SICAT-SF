package ar.sf.sicat.core.dominio.establecimiento;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EstablecimientoRepository
        extends JpaRepository<Establecimiento, Long>, JpaSpecificationExecutor<Establecimiento> {

    /**
     * Ids de los establecimientos en BORRADOR u OCULTO que cumplen los requisitos de publicación (ADR 0008).
     * Traducción a SQL de {@code PublicacionService.evaluar} (la fuente de la regla); un test compara las dos.
     */
    String LISTOS_PARA_PUBLICAR = """
            SELECT e.id_establecimiento FROM sicat.establecimiento e
            JOIN sicat.oferente ofe ON ofe.id_oferente = e.id_oferente
            JOIN sicat.lugar l ON l.id_lugar = e.id_lugar
            WHERE e.estado_publicacion IN ('BORRADOR', 'OCULTO')
              AND e.activo AND ofe.activo
              AND l.punto IS NOT NULL
              AND (l.estado_geo IS NULL OR l.estado_geo NOT IN ('AUTOMATICO', 'APROXIMADO'))
              AND EXISTS (SELECT 1 FROM sicat.oferta o
                          JOIN sicat.categoria c ON c.id_categoria = o.id_categoria
                          WHERE o.id_establecimiento = e.id_establecimiento AND o.activo AND c.activo)""";

    /**
     * Ids de establecimientos con algún contacto que no sirve para armar un buen enlace: Instagram que no es
     * usuario ni URL del perfil, Facebook que es un nombre y no la URL de la página (o un usuario), web con espacios.
     * Mismas reglas que {@code problemaDe} en frontend/projects/shared/src/lib/contactos.ts.
     * (Sin "?" en las expresiones: Hibernate lo tomaría como parámetro.)
     */
    String CONTACTOS_A_REVISAR = """
            SELECT DISTINCT c.id_establecimiento FROM sicat.canal_contacto c
            JOIN sicat.tipo_canal t ON t.id_tipo_canal = c.id_tipo_canal
            WHERE c.id_establecimiento IS NOT NULL AND btrim(c.valor) <> '' AND (
                 (lower(t.nombre) LIKE '%instagram%'
                  AND btrim(c.valor) !~* '^(http://|https://|)(www\\.|)instagram\\.com/'
                  AND btrim(c.valor) !~ '^(@|)[A-Za-z0-9._]{1,30}$')
              OR (lower(t.nombre) LIKE '%facebook%'
                  AND btrim(c.valor) !~* '^(http://|https://|)(www\\.|m\\.|web\\.|)(facebook|fb)\\.(com|me)/'
                  AND btrim(c.valor) !~ '^(@|)[A-Za-z0-9.]{5,}$')
              OR ((lower(t.nombre) LIKE '%web%' OR lower(t.nombre) LIKE '%sitio%')
                  AND btrim(c.valor) ~ '[[:space:]]'))""";

    /** Página del listado con oferente, lugar y localidad en la misma consulta (sin N+1 al armar las filas). */
    @Override
    @EntityGraph(attributePaths = {"oferente", "lugar", "lugar.localidad"})
    Page<Establecimiento> findAll(Specification<Establecimiento> spec, Pageable pageable);

    List<Establecimiento> findByOferenteIdOrderByNombre(Long idOferente);

    @Query(value = LISTOS_PARA_PUBLICAR, nativeQuery = true)
    List<Long> idsListosParaPublicar();

    /** [id, nombre] de los activos: grupos de posibles duplicados sin cargar entidades. */
    @Query("SELECT e.id, e.nombre FROM Establecimiento e WHERE e.activo = true")
    List<Object[]> idsYNombresActivos();

    /** Contadores del menú del panel, en una sola consulta. */
    @Query(value = "SELECT"
            + " (SELECT count(*) FROM sicat.lugar"
            + "   WHERE estado_geo IN ('AUTOMATICO', 'APROXIMADO', 'NO_ENCONTRADO', 'PENDIENTE')) AS ubicacionesARevisar,"
            + " (SELECT count(*) FROM (" + LISTOS_PARA_PUBLICAR + ") listos) AS listosParaPublicar,"
            + " (SELECT count(*) FROM sicat.establecimiento ea WHERE ea.activo AND ea.id_establecimiento IN ("
            + CONTACTOS_A_REVISAR + ")) AS contactosARevisar", nativeQuery = true)
    ContadoresPanel contadores();

    interface ContadoresPanel {
        long getUbicacionesARevisar();

        long getListosParaPublicar();

        long getContactosARevisar();
    }

    boolean existsByLugarId(Long idLugar);

    List<Establecimiento> findByLugarId(Long idLugar);

    /** Establecimientos con alguna oferta cuya categoría principal es la indicada. */
    @Query("SELECT DISTINCT o.establecimiento FROM Oferta o WHERE o.categoria.id = :idCategoria")
    List<Establecimiento> conOfertasDeCategoria(@Param("idCategoria") Long idCategoria);

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

    /** Ids de establecimientos cuyo lugar cae dentro del rectángulo (PostGIS). */
    @Query(value = """
            SELECT e.id_establecimiento FROM sicat.establecimiento e
            JOIN sicat.lugar l ON l.id_lugar = e.id_lugar
            WHERE l.punto IS NOT NULL
              AND public.ST_Intersects(l.punto,
                    public.ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326)::public.geography)""",
            nativeQuery = true)
    List<Long> idsEnBbox(@Param("minLng") double minLng, @Param("minLat") double minLat,
                         @Param("maxLng") double maxLng, @Param("maxLat") double maxLat);

    /** Ver {@link #CONTACTOS_A_REVISAR}. */
    @Query(value = CONTACTOS_A_REVISAR, nativeQuery = true)
    List<Long> idsConContactosARevisar();
}
