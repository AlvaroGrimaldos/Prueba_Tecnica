package com.acme.userauth.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/**
 * Reglas de dependencia entre capas, verificadas en build: las promesas de
 * Clean Architecture dejan de ser documentación y pasan a ser un test que
 * rompe la compilación de quien las viole.
 * <p>
 * Las reglas se evalúan desde tests Jupiter estándar ({@code rule.check})
 * en lugar del engine JUnit de ArchUnit: el engine no descubre los campos
 * {@code @ArchTest} con la versión de JUnit Platform que gestiona Spring
 * Boot 3.5, y esta forma es independiente del engine y se cuenta con
 * normalidad en Surefire.
 */
class ArchitectureTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.acme.userauth");

    /** El dominio no conoce a nadie: ni capas externas ni frameworks. */
    @Test
    void domainIsFrameworkFreeAndInnermost() {
        noClasses().that().resideInAPackage("..userauth.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..userauth.application..",
                        "..userauth.infrastructure..",
                        "..userauth.autoconfigure..",
                        "org.springframework..",
                        "jakarta.persistence..",
                        "jakarta.validation..")
                .check(CLASSES);
    }

    /** La aplicación no depende de los detalles de infraestructura. */
    @Test
    void applicationDoesNotDependOnInfrastructure() {
        noClasses().that().resideInAPackage("..userauth.application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..userauth.infrastructure..",
                        "..userauth.autoconfigure..")
                .check(CLASSES);
    }

    /** La aplicación no depende de JPA: la persistencia es un detalle. */
    @Test
    void applicationDoesNotDependOnJpa() {
        noClasses().that().resideInAPackage("..userauth.application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "jakarta.persistence..",
                        "org.springframework.data..")
                .check(CLASSES);
    }
}
