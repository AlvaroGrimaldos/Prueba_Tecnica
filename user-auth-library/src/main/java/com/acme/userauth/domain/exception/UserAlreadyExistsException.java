package com.acme.userauth.domain.exception;

/**
 * Se lanza al intentar registrar un usuario cuyo nombre de usuario o correo
 * electrónico ya están en uso.
 * <p>
 * Es una regla de negocio (unicidad del principal), por eso vive en el
 * dominio y no como error técnico de base de datos: la restricción UNIQUE en
 * PostgreSQL es la red de seguridad, no la regla.
 */
public class UserAlreadyExistsException extends RuntimeException {

    private UserAlreadyExistsException(String message) {
        super(message);
    }

    /** El nombre de usuario ya está registrado. */
    public static UserAlreadyExistsException byUsername(String username) {
        return new UserAlreadyExistsException("username '%s' is already taken".formatted(username));
    }

    /** El correo electrónico ya está registrado. */
    public static UserAlreadyExistsException byEmail(String email) {
        return new UserAlreadyExistsException("email '%s' is already registered".formatted(email));
    }
}
