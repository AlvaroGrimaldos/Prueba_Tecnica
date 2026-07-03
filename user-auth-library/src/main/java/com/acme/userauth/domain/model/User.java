package com.acme.userauth.domain.model;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import com.acme.userauth.domain.vo.Email;
import com.acme.userauth.domain.vo.PasswordHash;
import com.acme.userauth.domain.vo.UserId;
import com.acme.userauth.domain.vo.Username;

/**
 * Agregado raíz {@code User}: modelo rico de dominio, independiente de JPA y
 * de cualquier framework.
 * <p>
 * Sus invariantes se protegen mediante constructor privado y factorías:
 * <ul>
 *   <li>{@link #register(Username, Email, PasswordHash, Set)} — alta de un
 *       usuario nuevo, con identidad generada y estado inicial habilitado;</li>
 *   <li>{@link #reconstitute(UserId, Username, Email, PasswordHash, Set, boolean, Instant)}
 *       — rehidratación desde persistencia, reservada a la capa de
 *       infraestructura.</li>
 * </ul>
 * El estado solo cambia a través de métodos con significado de negocio
 * ({@link #changePassword(PasswordHash)}, {@link #disable()}…), nunca con
 * setters genéricos.
 */
public final class User {

    private final UserId id;
    private final Username username;
    private final Email email;
    private final Set<Role> roles;
    private final Instant createdAt;

    private PasswordHash passwordHash;
    private boolean enabled;

    private User(UserId id, Username username, Email email, PasswordHash passwordHash,
                 Set<Role> roles, boolean enabled, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.username = Objects.requireNonNull(username, "username must not be null");
        this.email = Objects.requireNonNull(email, "email must not be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(roles, "roles must not be null");
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("a user requires at least one role");
        }
        this.roles = EnumSet.copyOf(roles);
        this.enabled = enabled;
    }

    /**
     * Factoría de alta: crea un usuario nuevo, habilitado, con identidad
     * recién generada y fecha de creación actual.
     */
    public static User register(Username username, Email email,
                                PasswordHash passwordHash, Set<Role> roles) {
        return new User(UserId.newId(), username, email, passwordHash, roles, true, Instant.now());
    }

    /**
     * Factoría de rehidratación desde el almacén de datos. No aplica reglas
     * de alta: asume que el estado persistido ya fue validado en su día.
     * Uso reservado a los adaptadores de persistencia.
     */
    public static User reconstitute(UserId id, Username username, Email email,
                                    PasswordHash passwordHash, Set<Role> roles,
                                    boolean enabled, Instant createdAt) {
        return new User(id, username, email, passwordHash, roles, enabled, createdAt);
    }

    /**
     * Sustituye la contraseña por un hash nuevo ya codificado.
     * La verificación de la contraseña actual es responsabilidad del caso de
     * uso; el agregado solo garantiza que el nuevo hash es válido.
     */
    public void changePassword(PasswordHash newHash) {
        this.passwordHash = Objects.requireNonNull(newHash, "newHash must not be null");
    }

    /** Habilita la cuenta para iniciar sesión. */
    public void enable() {
        this.enabled = true;
    }

    /** Deshabilita la cuenta; un usuario deshabilitado no puede autenticarse. */
    public void disable() {
        this.enabled = false;
    }

    /** Indica si el usuario posee el rol dado. */
    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    public UserId id() {
        return id;
    }

    public Username username() {
        return username;
    }

    public Email email() {
        return email;
    }

    public PasswordHash passwordHash() {
        return passwordHash;
    }

    /** Vista inmutable de los roles del usuario. */
    public Set<Role> roles() {
        return Set.copyOf(roles);
    }

    public boolean enabled() {
        return enabled;
    }

    public Instant createdAt() {
        return createdAt;
    }

    /** Igualdad por identidad del agregado, no por atributos. */
    @Override
    public boolean equals(Object o) {
        return o instanceof User other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "User[id=%s, username=%s, enabled=%s]".formatted(id.value(), username.value(), enabled);
    }
}
