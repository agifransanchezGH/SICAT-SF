package ar.sf.sicat.core.dominio.contacto;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CanalContactoRepository extends JpaRepository<CanalContacto, Long> {

    List<CanalContacto> findByEstablecimientoIdOrderByOrden(Long idEstablecimiento);

    List<CanalContacto> findByOferenteIdOrderByOrden(Long idOferente);

    /** Contactos de varios establecimientos con su tipo, en una consulta (comparación de duplicados). */
    @org.springframework.data.jpa.repository.Query("SELECT c FROM CanalContacto c JOIN FETCH c.tipoCanal"
            + " WHERE c.establecimiento.id IN :ids ORDER BY c.orden, c.id")
    List<CanalContacto> deEstablecimientos(@org.springframework.data.repository.query.Param("ids") java.util.Collection<Long> ids);
}
