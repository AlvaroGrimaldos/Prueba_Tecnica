package com.acme.userauth.application.dto;

/**
 * Contrato base para toda solicitud de autenticación.
 * <p>
 * Cada mecanismo de autenticación define su propia implementación con las
 * credenciales que necesite (contraseña, token, certificado, etc.). Lo único
 * que la librería exige es poder identificar al principal que intenta
 * autenticarse.
 * <p>
 * Se recomienda implementar este contrato mediante {@code record}s inmutables,
 * por ejemplo:
 * <pre>{@code
 * public record PasswordAuthenticationRequest(String principal, char[] rawPassword)
 *         implements AuthenticationRequest { }
 * }</pre>
 *
 * @see com.acme.userauth.application.service.Authenticator
 */
public interface AuthenticationRequest {

    /**
     * Identificador del sujeto que intenta autenticarse (nombre de usuario,
     * correo electrónico, subject de un token, etc.).
     *
     * @return identificador del principal; nunca {@code null}
     */
    String principal();
}
