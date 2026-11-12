package ar.sf.sicat.core.dominio.contacto;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CanalContactoRepository extends JpaRepository<CanalContacto, Long> {

    List<CanalContacto> findByEstablecimientoIdOrderByOrden(Long idEstablecimiento);

    List<CanalContacto> findByOferenteIdOrderByOrden(Long idOferente);
}
