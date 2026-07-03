package com.acme.userauth.application.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementación de la restricción {@link StrongPassword}.
 * <p>
 * Devuelve {@code true} para valores {@code null}: la obligatoriedad se
 * declara por separado con {@code @NotBlank}, de forma que cada anotación
 * informa de su propio error (mensajes más precisos para el usuario).
 */
public final class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final int MIN_LENGTH = 8;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return value.length() >= MIN_LENGTH
                && value.chars().anyMatch(Character::isLowerCase)
                && value.chars().anyMatch(Character::isUpperCase)
                && value.chars().anyMatch(Character::isDigit);
    }
}
