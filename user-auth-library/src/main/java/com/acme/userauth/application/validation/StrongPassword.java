package com.acme.userauth.application.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Restricción de Bean Validation para contraseñas en claro recibidas en los
 * comandos de entrada.
 * <p>
 * Exige un mínimo de 8 caracteres con al menos una minúscula, una mayúscula y
 * un dígito. Es validación <em>sintáctica</em> de frontera; las políticas de
 * negocio adicionales (historial de contraseñas, expiración…) pertenecen al
 * dominio.
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "password must be at least 8 characters long and contain "
            + "lower case, upper case and numeric characters";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
