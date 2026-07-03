package com.acme.userauth.application.dto;

/**
 * Solicitud de autenticación por nombre de usuario y contraseña.
 *
 * @param principal   nombre de usuario introducido
 * @param rawPassword contraseña en claro introducida; nunca se persiste
 */
public record PasswordAuthenticationRequest(String principal, String rawPassword)
        implements AuthenticationRequest {

    /** Evita filtrar la contraseña en claro por logs o trazas. */
    @Override
    public String toString() {
        return "PasswordAuthenticationRequest[principal=%s, rawPassword=****]".formatted(principal);
    }
}
