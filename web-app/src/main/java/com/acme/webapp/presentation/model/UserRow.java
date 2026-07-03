package com.acme.webapp.presentation.model;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.stream.Collectors;

import com.acme.userauth.application.dto.UserResponse;

/**
 * Modelo de fila para la tabla de usuarios.
 * <p>
 * Adapta el DTO {@link UserResponse} de la librería a lo que la vista ZUL
 * necesita, por dos razones:
 * <ul>
 *   <li>{@code UserResponse} es un record y sus accesores ({@code username()})
 *       no siguen la convención JavaBean que espera el resolutor EL de ZK
 *       ({@code getUsername()});</li>
 *   <li>el formateo de presentación (roles como texto, fechas legibles) se
 *       centraliza aquí y no contamina ni la librería ni el ZUL.</li>
 * </ul>
 * Inmutable: la vista solo lee.
 */
public final class UserRow {

    private static final DateTimeFormatter CREATED_AT_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final UUID id;
    private final String username;
    private final String email;
    private final String rolesLabel;
    private final boolean enabled;
    private final String createdAtLabel;

    private UserRow(UUID id, String username, String email,
                    String rolesLabel, boolean enabled, String createdAtLabel) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.rolesLabel = rolesLabel;
        this.enabled = enabled;
        this.createdAtLabel = createdAtLabel;
    }

    /** Adapta el DTO de la librería a la fila que consume la vista. */
    public static UserRow from(UserResponse user) {
        return new UserRow(
                user.id(),
                user.username(),
                user.email(),
                user.roles().stream().map(Enum::name).sorted().collect(Collectors.joining(", ")),
                user.enabled(),
                CREATED_AT_FORMAT.format(user.createdAt()));
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getRolesLabel() {
        return rolesLabel;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getCreatedAtLabel() {
        return createdAtLabel;
    }
}
