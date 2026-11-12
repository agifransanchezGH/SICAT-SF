package ar.sf.sicat.admin.auditoria;

import java.util.List;
import java.util.Map;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

/**
 * Registra {@link AuditoriaListener} en Hibernate al crear la SessionFactory de la API Admin.
 * Vive en el paquete de auditoría para que ni la configuración ni el dominio la referencien.
 */
@Component
class AuditoriaIntegrador implements HibernatePropertiesCustomizer, Integrator {

    private final AuditoriaListener listener;

    AuditoriaIntegrador(AuditoriaListener listener) {
        this.listener = listener;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put("hibernate.integrator_provider", (IntegratorProvider) () -> List.of(this));
    }

    @Override
    public void integrate(Metadata metadata, BootstrapContext bootstrapContext, SessionFactoryImplementor sessionFactory) {
        EventListenerRegistry registro = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);
        registro.appendListeners(EventType.POST_INSERT, listener);
        registro.appendListeners(EventType.POST_UPDATE, listener);
        registro.appendListeners(EventType.POST_DELETE, listener);
        registro.appendListeners(EventType.POST_COLLECTION_UPDATE, listener);
        registro.appendListeners(EventType.POST_COLLECTION_RECREATE, listener);
    }

    @Override
    public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
        // nada que liberar
    }
}
