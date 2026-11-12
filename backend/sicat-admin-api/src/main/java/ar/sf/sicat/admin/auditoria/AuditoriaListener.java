package ar.sf.sicat.admin.auditoria;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.event.spi.AbstractCollectionEvent;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostCollectionRecreateEvent;
import org.hibernate.event.spi.PostCollectionRecreateEventListener;
import org.hibernate.event.spi.PostCollectionUpdateEvent;
import org.hibernate.event.spi.PostCollectionUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.locationtech.jts.geom.Geometry;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ar.sf.sicat.core.auditoria.AccionAuditoria;
import ar.sf.sicat.core.auditoria.Auditoria;
import ar.sf.sicat.core.auditoria.OrigenAuditoria;

/**
 * Registra en auditoria.auditoria cada alta, modificación y baja de entidades JPA, en la misma
 * transacción que el cambio (si la transacción se revierte, la auditoría también).
 * <p>
 * Se engancha a los eventos de Hibernate (ver {@link AuditoriaIntegrador}); las entidades y los
 * servicios de negocio no saben que existe (regla 5 de ARQUITECTURA.md).
 * <ul>
 *   <li>ALTA: {@code datosNuevos} con todos los campos.</li>
 *   <li>MODIFICACION: solo los campos que cambiaron, antes y después. Cambios en colecciones
 *       (p. ej. categorías adicionales) se registran como MODIFICACION del dueño con la lista de ids.</li>
 *   <li>ELIMINACION: {@code datosAnteriores} con todos los campos.</li>
 * </ul>
 * Las asociaciones se guardan como el id de la entidad referida; las geometrías como WKT.
 * {@code hashPassword} nunca se guarda (se anota "(modificada)") y {@code ultimoAcceso} se ignora
 * para que cada login no genere un registro.
 */
@Component
class AuditoriaListener implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener,
        PostCollectionUpdateEventListener, PostCollectionRecreateEventListener {

    private static final String SQL = """
            INSERT INTO auditoria.auditoria
                (tabla_afectada, clave_registro, accion, id_usuario, fecha_hora, origen, datos_anteriores, datos_nuevos)
            VALUES (?, ?, ?, ?, clock_timestamp(), ?, ?::jsonb, ?::jsonb)""";

    private static final Set<String> IGNORADOS = Set.of("ultimoAcceso");
    private static final Set<String> SENSIBLES = Set.of("hashPassword");
    private static final String MODIFICADA = "(modificada)";

    private final ObjectMapper json;

    AuditoriaListener(ObjectMapper json) {
        this.json = json;
    }

    // ---------------------------------------------------------------- entidades

    @Override
    public void onPostInsert(PostInsertEvent event) {
        if (noAuditable(event.getEntity())) {
            return;
        }
        Map<String, Object> nuevos = estado(event.getPersister(), event.getState(), null, event.getSession());
        registrar(event.getSession(), event.getPersister(), event.getId(), AccionAuditoria.ALTA, null, nuevos);
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        if (noAuditable(event.getEntity())) {
            return;
        }
        EntityPersister persister = event.getPersister();
        Object[] antes = event.getOldState();
        Object[] despues = event.getState();
        List<Integer> cambiados = cambiados(persister, antes, despues, event.getDirtyProperties(), event.getSession());
        if (cambiados.isEmpty()) {
            return;
        }
        Map<String, Object> anteriores = antes == null ? null : estado(persister, antes, cambiados, event.getSession());
        Map<String, Object> nuevos = estado(persister, despues, cambiados, event.getSession());
        registrar(event.getSession(), persister, event.getId(), AccionAuditoria.MODIFICACION, anteriores, nuevos);
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        if (noAuditable(event.getEntity())) {
            return;
        }
        Map<String, Object> anteriores = estado(event.getPersister(), event.getDeletedState(), null, event.getSession());
        registrar(event.getSession(), event.getPersister(), event.getId(), AccionAuditoria.ELIMINACION, anteriores, null);
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }

    // ---------------------------------------------------------------- colecciones (@ManyToMany)

    @Override
    public void onPostUpdateCollection(PostCollectionUpdateEvent event) {
        PersistentCollection<?> coleccion = event.getCollection();
        List<Object> antes = ids(snapshot(coleccion.getStoredSnapshot()), event.getSession());
        registrarColeccion(event, antes);
    }

    @Override
    public void onPostRecreateCollection(PostCollectionRecreateEvent event) {
        if (elementos(event.getCollection()).isEmpty()) {
            return;
        }
        registrarColeccion(event, null);
    }

    private void registrarColeccion(AbstractCollectionEvent event, List<Object> antes) {
        Object duenio = event.getAffectedOwnerOrNull();
        if (duenio == null || noAuditable(duenio)) {
            return;
        }
        List<Object> despues = ids(elementos(event.getCollection()), event.getSession());
        if (Objects.equals(antes, despues)) {
            return;
        }
        String rol = event.getCollection().getRole();
        String campo = rol.substring(rol.lastIndexOf('.') + 1);
        Map<String, Object> anteriores = null;
        if (antes != null) {
            anteriores = new LinkedHashMap<>();
            anteriores.put(campo, antes);
        }
        Map<String, Object> nuevos = new LinkedHashMap<>();
        nuevos.put(campo, despues);
        EntityPersister persister = event.getSession().getEntityPersister(event.getAffectedOwnerEntityName(), duenio);
        registrar(event.getSession(), persister, event.getAffectedOwnerIdOrNull(),
                AccionAuditoria.MODIFICACION, anteriores, nuevos);
    }

    // ---------------------------------------------------------------- armado del registro

    private static boolean noAuditable(Object entidad) {
        return entidad instanceof Auditoria;
    }

    /** Índices de las propiedades que cambiaron, sin colecciones ni campos ignorados. */
    private List<Integer> cambiados(EntityPersister persister, Object[] antes, Object[] despues,
                                    int[] sucios, EventSource session) {
        String[] nombres = persister.getPropertyNames();
        Type[] tipos = persister.getPropertyTypes();
        List<Integer> resultado = new ArrayList<>();
        for (int i = 0; i < nombres.length; i++) {
            if (tipos[i].isCollectionType() || IGNORADOS.contains(nombres[i])) {
                continue;
            }
            boolean cambio;
            if (sucios != null) {
                final int indice = i;
                cambio = java.util.Arrays.stream(sucios).anyMatch(s -> s == indice);
            } else if (antes == null) {
                cambio = true;
            } else {
                cambio = !Objects.equals(valor(antes[i], tipos[i], session), valor(despues[i], tipos[i], session));
            }
            if (cambio) {
                resultado.add(i);
            }
        }
        return resultado;
    }

    /** Estado como mapa nombre → valor serializable. {@code soloIndices} null = todas las propiedades. */
    private Map<String, Object> estado(EntityPersister persister, Object[] valores, List<Integer> soloIndices,
                                       EventSource session) {
        String[] nombres = persister.getPropertyNames();
        Type[] tipos = persister.getPropertyTypes();
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (int i = 0; i < nombres.length; i++) {
            if ((soloIndices != null && !soloIndices.contains(i))
                    || tipos[i].isCollectionType() || IGNORADOS.contains(nombres[i])) {
                continue;
            }
            mapa.put(nombres[i], SENSIBLES.contains(nombres[i]) ? MODIFICADA : valor(valores[i], tipos[i], session));
        }
        return mapa;
    }

    private static Object valor(Object v, Type tipo, EventSource session) {
        if (v == null) {
            return null;
        }
        if (tipo.isEntityType()) {
            return session.getFactory().getPersistenceUnitUtil().getIdentifier(v);
        }
        if (v instanceof Geometry g) {
            return g.toText();
        }
        if (v instanceof Enum<?> e) {
            return e.name();
        }
        return v;
    }

    private static Collection<?> snapshot(Object snapshot) {
        if (snapshot instanceof Map<?, ?> m) {
            return m.values();
        }
        if (snapshot instanceof Collection<?> c) {
            return c;
        }
        return List.of();
    }

    private static Collection<?> elementos(PersistentCollection<?> coleccion) {
        return coleccion instanceof Collection<?> c ? c : List.of();
    }

    private static List<Object> ids(Collection<?> entidades, EventSource session) {
        return entidades.stream()
                .map(e -> session.getFactory().getPersistenceUnitUtil().getIdentifier(e))
                .sorted((a, b) -> String.valueOf(a).compareTo(String.valueOf(b)))
                .toList();
    }

    private void registrar(EventSource session, EntityPersister persister, Object id, AccionAuditoria accion,
                           Map<String, Object> anteriores, Map<String, Object> nuevos) {
        String tabla = persister instanceof AbstractEntityPersister p ? p.getTableName() : persister.getEntityName();
        String clave = clave(id);
        String jsonAnteriores = aJson(anteriores);
        String jsonNuevos = aJson(nuevos);
        Long idUsuario = ContextoAuditoria.idUsuarioActual();
        OrigenAuditoria origen = ContextoAuditoria.origenActual();
        session.doWork(conexion -> {
            try (PreparedStatement ps = conexion.prepareStatement(SQL)) {
                ps.setString(1, tabla);
                ps.setString(2, clave);
                ps.setString(3, accion.name());
                if (idUsuario == null) {
                    ps.setNull(4, Types.BIGINT);
                } else {
                    ps.setLong(4, idUsuario);
                }
                ps.setString(5, origen.name());
                ps.setString(6, jsonAnteriores);
                ps.setString(7, jsonNuevos);
                ps.executeUpdate();
            }
        });
    }

    /** Id simple como texto; id compuesto como JSON (p. ej. {"idEstablecimiento":1,"idDistincion":2}). */
    private String clave(Object id) {
        if (id instanceof Number || id instanceof CharSequence) {
            return id.toString();
        }
        return aJson(id);
    }

    private String aJson(Object valor) {
        if (valor == null) {
            return null;
        }
        try {
            return json.writeValueAsString(valor);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar la auditoría", e);
        }
    }
}
