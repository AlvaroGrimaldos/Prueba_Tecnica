package com.acme.userauth.application.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.acme.userauth.domain.model.Role;
import com.acme.userauth.domain.model.User;

/**
 * Representación de salida de un usuario hacia las capas cliente.
 * <p>
 * Expone únicamente datos seguros: nunca incluye el hash de contraseña. Los
 * value objects del dominio se aplanan a tipos estándar para que los
 * consumidores (una vista ZK, un controlador REST…) no dependan del modelo
 * interno de la librería.
 *
 * @param id        identificador del usuario
 * @param username  nombre de usuario normalizado
 * @param email     correo electrónico normalizado
 * @param roles     roles asignados
 * @param enabled   si la cuenta puede autenticarse
 * @param createdAt instante de alta
 */
public record UserResponse(
        UUID id,
        String username,
        String email,
        Set<Role> roles,
        boolean enabled,
        Instant createdAt) {

    /** Mapea el agregado de dominio a su representación externa. */
    public static UserResponse from(User user) {
        return new UserResponse(
                user.id().value(),
                user.username().value(),
                user.email().value(),
                user.roles(),
                user.enabled(),
                user.createdAt());
    }
}
