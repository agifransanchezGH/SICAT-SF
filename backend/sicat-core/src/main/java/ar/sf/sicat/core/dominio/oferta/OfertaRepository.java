package ar.sf.sicat.core.dominio.oferta;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OfertaRepository extends JpaRepository<Oferta, Long> {

    List<Oferta> findByEstablecimientoIdOrderById(Long idEstablecimiento);

    /** Ofertas de varios establecimientos con su categoría y adicionales, en una consulta (para listados). */
    @Query("SELECT DISTINCT o FROM Oferta o JOIN FETCH o.categoria LEFT JOIN FETCH o.categoriasAdicionales"
            + " WHERE o.establecimiento.id IN :ids ORDER BY o.id")
    List<Oferta> conCategoriasDe(@Param("ids") Collection<Long> ids);
}
