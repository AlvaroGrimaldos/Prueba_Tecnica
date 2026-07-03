package com.acme.userauth.domain.vo;

import java.util.Objects;

/**
 * Hash de contraseña ya codificado (BCrypt, Argon2…).
 * <p>
 * El dominio nunca manipula contraseñas en claro: la codificación ocurre en
 * la capa de aplicación a través del puerto {@code PasswordEncoderPort}, y el
 * agregado {@code User} solo conoce este value object.
 * <p>
 * {@link #toString()} está sobrescrito para que el hash no acabe en logs ni
 * trazas de error por accidente.
 *
 * @param value hash codificado; nunca nulo ni vacío
 */
public record PasswordHash(String value) {

    public PasswordHash {
        Objects.requireNonNull(value, "password hash must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("password hash must not be blank");
        }
    }

    @Override
    public String toString() {
        return "PasswordHash[****]";
    }
}
