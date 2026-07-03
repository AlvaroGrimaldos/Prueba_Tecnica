package com.acme.userauth.domain.exception;

import java.util.UUID;

/**
 * Se lanza cuando se solicita un usuario que no existe en el sistema.
 * <p>
 * Las factorías estáticas garantizan mensajes consistentes y evitan que cada
 * llamante componga el suyo propio.
 */
public class UserNotFoundException extends RuntimeException {

    private UserNotFoundException(String message) {
        super(message);
    }

    /** Usuario inexistente buscado por identificador. */
    public static UserNotFoundException byId(UUID id) {
        return new UserNotFoundException("user with id '%s' was not found".formatted(id));
    }

    /** Usuario inexistente buscado por nombre de usuario. */
    public static UserNotFoundException byUsername(String username) {
        return new UserNotFoundException("user with username '%s' was not found".formatted(username));
    }
}
