package com.acme.userauth.infrastructure.persistence.mapper;

import com.acme.userauth.domain.model.User;
import com.acme.userauth.domain.vo.Email;
import com.acme.userauth.domain.vo.PasswordHash;
import com.acme.userauth.domain.vo.UserId;
import com.acme.userauth.domain.vo.Username;
import com.acme.userauth.infrastructure.persistence.entity.UserEntity;

/**
 * Traduce entre el agregado de dominio {@link User} y la entidad JPA
 * {@link UserEntity}.
 * <p>
 * Mapper manual y sin estado: para dos tipos con un puñado de campos, el
 * código explícito es más fácil de depurar que un generador (MapStruct
 * seguiría siendo una mejora válida si el modelo creciera).
 */
public class UserPersistenceMapper {

    /** Rehidrata el agregado de dominio desde su representación persistida. */
    public User toDomain(UserEntity entity) {
        return User.reconstitute(
                new UserId(entity.getId()),
                new Username(entity.getUsername()),
                new Email(entity.getEmail()),
                new PasswordHash(entity.getPasswordHash()),
                entity.getRoles(),
                entity.isEnabled(),
                entity.getCreatedAt());
    }

    /** Crea una entidad nueva a partir de un agregado aún no persistido. */
    public UserEntity toNewEntity(User user) {
        return new UserEntity(
                user.id().value(),
                user.username().value(),
                user.email().value(),
                user.passwordHash().value(),
                user.roles(),
                user.enabled(),
                user.createdAt());
    }

    /**
     * Copia el estado mutable del agregado sobre una entidad ya gestionada
     * por el contexto de persistencia. Al reutilizar la instancia cargada se
     * preserva el campo {@code version} y, con él, el bloqueo optimista.
     */
    public void copyToEntity(User user, UserEntity target) {
        target.setUsername(user.username().value());
        target.setEmail(user.email().value());
        target.setPasswordHash(user.passwordHash().value());
        target.setRoles(user.roles());
        target.setEnabled(user.enabled());
    }
}
