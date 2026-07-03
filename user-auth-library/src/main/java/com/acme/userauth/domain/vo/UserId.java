package com.acme.userauth.domain.vo;

import java.util.Objects;
import java.util.UUID;

/**
 * Identificador único e inmutable de un {@code User}.
 * <p>
 * Envolver el {@link UUID} en un value object evita el "primitive obsession":
 * un {@code UserId} no puede confundirse con el identificador de otro
 * agregado aunque ambos sean UUID por debajo.
 *
 * @param value valor interno del identificador; nunca {@code null}
 */
public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "user id must not be null");
    }

    /** Genera un identificador nuevo y aleatorio para un usuario recién registrado. */
    public static UserId newId() {
        return new UserId(UUID.randomUUID());
    }

    /**
     * Reconstruye el identificador desde su representación textual.
     *
     * @throws IllegalArgumentException si el texto no es un UUID válido
     */
    public static UserId of(String raw) {
        return new UserId(UUID.fromString(raw));
    }
}
