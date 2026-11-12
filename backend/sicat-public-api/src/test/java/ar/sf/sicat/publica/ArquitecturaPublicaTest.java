package ar.sf.sicat.publica;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Regla 3 y regla 1 de ARQUITECTURA.md §3. */
@AnalyzeClasses(packages = "ar.sf.sicat", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaPublicaTest {

    @ArchTest
    static final ArchRule noDependeDeAdmin = noClasses()
            .that().resideInAPackage("ar.sf.sicat.publica..")
            .should().dependOnClassesThat().resideInAPackage("ar.sf.sicat.admin..")
            .because("sicat-public-api nunca depende de sicat-admin-api (regla 3)");

    @ArchTest
    static final ArchRule soloUsaVistasPublicas = noClasses()
            .that().resideInAPackage("ar.sf.sicat.publica..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "ar.sf.sicat.core.dominio..",
                    "ar.sf.sicat.core.seguridad..",
                    "ar.sf.sicat.core.auditoria..")
            .because("la API Pública solo lee las vistas del esquema publico (regla 1)");
}
