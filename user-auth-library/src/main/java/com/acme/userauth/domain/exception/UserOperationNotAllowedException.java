package com.acme.userauth.domain.exception;

/**
 * Se lanza cuando una operación sobre un usuario, aun siendo técnicamente
 * posible, violaría una regla de negocio de protección del sistema.
 */
public class UserOperationNotAllowedException extends RuntimeException {

    private UserOperationNotAllowedException(String message) {
        super(message);
    }

    /**
     * Protección contra el bloqueo total del sistema: el último administrador
     * habilitado no puede deshabilitarse ni eliminarse.
     */
    public static UserOperationNotAllowedException lastAdministrator(String username) {
        return new UserOperationNotAllowedException(
                "'%s' is the last enabled administrator and cannot be disabled or deleted"
                        .formatted(username));
    }
}
