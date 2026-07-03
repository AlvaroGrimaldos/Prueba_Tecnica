package com.acme.userauth.domain.exception;

/**
 * Excepción base de la jerarquía de errores de autenticación.
 * <p>
 * Las implementaciones concretas de {@code Authenticator} deben lanzar esta
 * excepción (o subclases más específicas, como «credenciales inválidas» o
 * «cuenta bloqueada») cuando el proceso de autenticación falle por una razón
 * de negocio.
 * <p>
 * Es una excepción no comprobada ({@link RuntimeException}) para no obligar a
 * las capas superiores a propagar {@code throws} por toda la aplicación;
 * el punto de captura natural es la plantilla
 * {@code Authenticator#authenticate}, que la traduce a un resultado fallido.
 */
public class AuthenticationException extends RuntimeException {

    /**
     * Crea la excepción con un mensaje descriptivo del fallo.
     *
     * @param message descripción del motivo del fallo; se usará como
     *                {@code failureReason} del resultado de autenticación
     */
    public AuthenticationException(String message) {
        super(message);
    }

    /**
     * Crea la excepción con un mensaje y la causa original del fallo.
     *
     * @param message descripción del motivo del fallo
     * @param cause   causa raíz (p. ej. un error del proveedor externo)
     */
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
