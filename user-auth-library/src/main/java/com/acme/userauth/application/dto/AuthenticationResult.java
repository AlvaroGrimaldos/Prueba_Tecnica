package com.acme.userauth.application.dto;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * Resultado inmutable de un intento de autenticación.
 * <p>
 * Modela tanto el éxito como el fallo de forma explícita, evitando que las
 * capas cliente dependan de excepciones para el flujo de control. Se
 * construye únicamente a través de las factorías {@link #success(String, Set)}
 * y {@link #failure(String, String)} para garantizar estados válidos.
 *
 * @param authenticated {@code true} si la autenticación fue satisfactoria
 * @param principal     identificador del sujeto autenticado (o que lo intentó)
 * @param roles         roles concedidos al principal; vacío si falló
 * @param failureReason motivo del fallo; {@code null} si fue satisfactoria
 * @param timestamp     instante UTC en que se resolvió el intento
 */
public record AuthenticationResult(
        boolean authenticated,
        String principal,
        Set<String> roles,
        String failureReason,
        Instant timestamp) {

    /**
     * Constructor canónico compacto: valida invariantes y hace copia
     * defensiva e inmutable de los roles.
     *
     * @throws NullPointerException     si {@code principal}, {@code roles} o
     *                                  {@code timestamp} son {@code null}
     * @throws IllegalArgumentException si el estado es inconsistente (éxito
     *                                  con motivo de fallo, o fallo sin él)
     */
    public AuthenticationResult {
        Objects.requireNonNull(principal, "principal must not be null");
        Objects.requireNonNull(roles, "roles must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        if (authenticated && failureReason != null) {
            throw new IllegalArgumentException("a successful result cannot carry a failure reason");
        }
        if (!authenticated && (failureReason == null || failureReason.isBlank())) {
            throw new IllegalArgumentException("a failed result requires a failure reason");
        }
        roles = Set.copyOf(roles);
    }

    /**
     * Crea un resultado satisfactorio.
     *
     * @param principal identificador del sujeto autenticado
     * @param roles     roles concedidos; puede ser vacío, no {@code null}
     * @return resultado con {@code authenticated == true}
     */
    public static AuthenticationResult success(String principal, Set<String> roles) {
        return new AuthenticationResult(true, principal, roles, null, Instant.now());
    }

    /**
     * Crea un resultado fallido.
     *
     * @param principal identificador del sujeto que intentó autenticarse
     * @param reason    motivo del fallo, apto para registrar en auditoría
     *                  (no debe exponer información sensible al usuario final)
     * @return resultado con {@code authenticated == false}
     */
    public static AuthenticationResult failure(String principal, String reason) {
        return new AuthenticationResult(false, principal, Set.of(), reason, Instant.now());
    }
}
