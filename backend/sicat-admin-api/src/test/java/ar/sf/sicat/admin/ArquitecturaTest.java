package ar.sf.sicat.admin;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.springframework.web.bind.annotation.RestController;

import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import jakarta.persistence.Entity;

/** Reglas de ARQUITECTURA.md §3 verificadas en cada build. */
@AnalyzeClasses(packages = "ar.sf.sicat", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    /** Regla 5: la auditoría está separada de la lógica de negocio. */
    @ArchTest
    static final ArchRule dominioNoDependeDeAuditoria = noClasses()
            .that().resideInAnyPackage("ar.sf.sicat.core.dominio..")
            .should().dependOnClassesThat().resideInAnyPackage("..auditoria..")
            .because("la auditoría no debe referenciarse desde el dominio (regla 5)");

    @ArchTest
    static final ArchRule featuresAdminNoDependenDeAuditoria = noClasses()
            .that().resideInAPackage("ar.sf.sicat.admin..")
            .and().resideOutsideOfPackages("ar.sf.sicat.admin.auditoria..", "ar.sf.sicat.admin.config..")
            .should().dependOnClassesThat().resideInAnyPackage("..auditoria..")
            .because("los servicios de negocio no conocen la auditoría (regla 5)")
            .allowEmptyShould(true);

    /** Regla 4: ninguna entidad JPA se expone por HTTP. */
    @ArchTest
    static final ArchRule controllersNoDevuelvenEntidades = methods()
            .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
            .and().arePublic()
            .should(noDevolverEntidades())
            .because("se exponen DTOs, nunca entidades JPA (regla 4)")
            .allowEmptyShould(true);

    /** El core no conoce a las APIs. */
    @ArchTest
    static final ArchRule coreNoDependeDeApis = noClasses()
            .that().resideInAPackage("ar.sf.sicat.core..")
            .should().dependOnClassesThat().resideInAnyPackage("ar.sf.sicat.admin..", "ar.sf.sicat.publica..");

    private static ArchCondition<JavaMethod> noDevolverEntidades() {
        return new ArchCondition<>("no devolver entidades JPA") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                boolean devuelveEntidad = method.getReturnType().getAllInvolvedRawTypes().stream()
                        .anyMatch(t -> t.isAnnotatedWith(Entity.class));
                if (devuelveEntidad) {
                    events.add(SimpleConditionEvent.violated(method, method.getFullName() + " devuelve una entidad JPA"));
                }
            }
        };
    }
}
