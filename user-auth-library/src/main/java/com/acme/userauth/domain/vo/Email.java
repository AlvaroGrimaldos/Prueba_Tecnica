package com.acme.userauth.domain.vo;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Dirección de correo electrónico normalizada y siempre válida.
 * <p>
 * La validación es deliberadamente pragmática (estructura
 * {@code local@dominio.tld}); la verificación definitiva de un correo solo
 * puede hacerse enviando un mensaje, por lo que un regex exhaustivo del
 * RFC 5322 aporta complejidad sin garantías adicionales.
 *
 * @param value dirección normalizada en minúsculas
 */
public record Email(String value) {

    private static final Pattern VALID =
            Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private static final int MAX_LENGTH = 254;

    public Email {
        Objects.requireNonNull(value, "email must not be null");
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_LENGTH || !VALID.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid email address: " + value);
        }
    }
}
