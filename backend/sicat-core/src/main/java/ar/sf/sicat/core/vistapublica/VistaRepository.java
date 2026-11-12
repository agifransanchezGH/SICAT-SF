package ar.sf.sicat.core.vistapublica;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

/** Base de los repositorios sobre vistas: solo lectura (no expone save ni delete). */
@NoRepositoryBean
public interface VistaRepository<T, ID> extends Repository<T, ID> {

    Optional<T> findById(ID id);

    List<T> findAll(Sort sort);

    Page<T> findAll(Pageable pageable);

    long count();
}
