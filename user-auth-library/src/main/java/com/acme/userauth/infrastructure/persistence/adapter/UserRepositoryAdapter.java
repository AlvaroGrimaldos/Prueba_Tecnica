package com.acme.userauth.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.acme.userauth.application.dto.PageResult;
import com.acme.userauth.application.port.out.UserRepositoryPort;
import com.acme.userauth.domain.exception.UserAlreadyExistsException;
import com.acme.userauth.domain.model.Role;
import com.acme.userauth.domain.model.User;
import com.acme.userauth.domain.vo.Email;
import com.acme.userauth.domain.vo.UserId;
import com.acme.userauth.domain.vo.Username;
import com.acme.userauth.infrastructure.persistence.entity.UserEntity;
import com.acme.userauth.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.acme.userauth.infrastructure.persistence.repository.SpringDataUserRepository;

/**
 * Adaptador de persistencia: implementa el puerto de salida
 * {@link UserRepositoryPort} definido por la capa de aplicación, delegando en
 * Spring Data JPA.
 * <p>
 * Es el único punto donde el mundo del dominio y el mundo JPA se tocan; si
 * mañana la persistencia cambiara (otra base de datos, un servicio remoto),
 * solo este paquete se vería afectado.
 */
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final SpringDataUserRepository repository;
    private final UserPersistenceMapper mapper;

    public UserRepositoryAdapter(SpringDataUserRepository repository, UserPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Distingue alta de actualización: en la actualización reutiliza la
     * entidad gestionada para conservar la columna {@code version} del
     * bloqueo optimista.
     * <p>
     * Usa {@code saveAndFlush} para que las constraints UNIQUE se evalúen
     * dentro de este método (y no al commit de la transacción), de forma que
     * una carrera entre dos registros concurrentes se traduzca aquí mismo a
     * {@link UserAlreadyExistsException} en lugar de escapar como error
     * técnico de infraestructura.
     */
    @Override
    public User save(User user) {
        UserEntity entity = repository.findById(user.id().value())
                .map(existing -> {
                    mapper.copyToEntity(user, existing);
                    return existing;
                })
                .orElseGet(() -> mapper.toNewEntity(user));
        try {
            return mapper.toDomain(repository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException ex) {
            throw translateIntegrityViolation(user, ex);
        }
    }

    /**
     * Traduce una violación de integridad a la excepción de dominio
     * correspondiente usando los nombres de constraint definidos en la
     * migración {@code V1__create_users.sql} (contrato librería-esquema).
     */
    private RuntimeException translateIntegrityViolation(User user, DataIntegrityViolationException ex) {
        var message = ex.getMostSpecificCause().getMessage();
        if (message != null) {
            var normalized = message.toLowerCase();
            if (normalized.contains("uk_users_username")) {
                return UserAlreadyExistsException.byUsername(user.username().value());
            }
            if (normalized.contains("uk_users_email")) {
                return UserAlreadyExistsException.byEmail(user.email().value());
            }
        }
        return ex;
    }

    @Override
    public Optional<User> findById(UserId id) {
        return repository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(Username username) {
        return repository.findByUsername(username.value()).map(mapper::toDomain);
    }

    @Override
    public boolean existsByUsername(Username username) {
        return repository.existsByUsername(username.value());
    }

    @Override
    public boolean existsByEmail(Email email) {
        return repository.existsByEmail(email.value());
    }

    @Override
    public List<User> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public PageResult<User> search(String filter, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "username"));
        Page<UserEntity> result = filter.isBlank()
                ? repository.findAll(pageable)
                : repository.findByUsernameContainingOrEmailContaining(filter, filter, pageable);
        return new PageResult<>(
                result.getContent().stream().map(mapper::toDomain).toList(),
                result.getTotalElements(),
                page,
                size);
    }

    @Override
    public long countEnabledUsersWithRole(Role role) {
        return repository.countEnabledUsersWithRole(role);
    }

    @Override
    public void deleteById(UserId id) {
        repository.deleteById(id.value());
    }
}
