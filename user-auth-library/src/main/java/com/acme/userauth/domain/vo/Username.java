package com.acme.userauth.domain.vo;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Nombre de usuario normalizado y siempre válido.
 * <p>
 * La invariante se garantiza en construcción (principio "always-valid
 * domain"): si existe una instancia de {@code Username}, es válida. La
 * normalización (trim + minúsculas) asegura que las comparaciones de unicidad
 * sean consistentes.
 *
 * @param value nombre normalizado: 3–50 caracteres alfanuméricos, punto,
 *              guion o guion bajo
 */
public record Username(String value) {

    private static final Pattern VALID = Pattern.compile("^[a-z0-9._-]{3,50}$");

    public Username {
        Objects.requireNonNull(value, "username must not be null");
        value = value.trim().toLowerCase(Locale.ROOT);
        if (!VALID.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "username must be 3-50 characters long and contain only letters, digits, '.', '_' or '-'");
        }
    }
}
