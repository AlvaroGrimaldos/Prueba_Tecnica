package com.acme.userauth.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.BatchSize;

import com.acme.userauth.domain.model.Role;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Entidad JPA de la tabla {@code users}.
 * <p>
 * Modelo de <em>persistencia</em>, deliberadamente separado del agregado de
 * dominio {@code User}: aquí viven las anotaciones JPA, el constructor
 * sin argumentos que exige el proveedor y los setters que necesita el mapper.
 * El dominio permanece libre de todo ello. La traducción entre ambos la
 * realiza {@code UserPersistenceMapper}.
 * <p>
 * El campo {@link Version @Version} habilita bloqueo optimista frente a
 * actualizaciones concurrentes.
 */
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // BatchSize evita el N+1 al listar: los roles de hasta 50 usuarios se
    // cargan en una única consulta IN en lugar de una consulta por usuario
    @ElementCollection(fetch = FetchType.EAGER)
    @BatchSize(size = 50)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private Set<Role> roles = new HashSet<>();

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Wrapper (no primitivo) a propósito: como el UUID lo asigna el dominio,
    // Spring Data no puede usar "id == null" para detectar entidades nuevas;
    // con la versión nullable usa "version == null" y persiste directamente
    // sin el SELECT extra de un merge
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** Requerido por JPA; no usar directamente. */
    protected UserEntity() {
    }

    public UserEntity(UUID id, String username, String email, String passwordHash,
                      Set<Role> roles, boolean enabled, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roles = new HashSet<>(roles);
        this.enabled = enabled;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = new HashSet<>(roles);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Long getVersion() {
        return version;
    }
}
