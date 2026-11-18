package ar.sf.sicat.core.dominio.clasificacion;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    /** Categorías que tienen asociada la característica (tabla categoria_caracteristica). */
    List<Categoria> findByCaracteristicasId(Long idCaracteristica);

    /** Cantidad de categorías por característica: filas [id_caracteristica, cantidad]. */
    @Query("SELECT c.id, COUNT(cat) FROM Categoria cat JOIN cat.caracteristicas c GROUP BY c.id")
    List<Object[]> contarPorCaracteristica();
}
