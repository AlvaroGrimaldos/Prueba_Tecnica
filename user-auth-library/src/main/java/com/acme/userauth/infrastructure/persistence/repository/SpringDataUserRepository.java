package com.acme.userauth.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.acme.userauth.domain.model.Role;
import com.acme.userauth.infrastructure.persistence.entity.UserEntity;

/**
 * Repositorio Spring Data JPA sobre {@link UserEntity}.
 * <p>
 * Detalle interno de infraestructura: ninguna otra capa lo conoce. La capa de
 * aplicación trabaja contra {@code UserRepositoryPort}, cuya implementación
 * ({@code UserRepositoryAdapter}) delega aquí. Las consultas son derivadas
 * del nombre del método; los valores llegan ya normalizados (minúsculas)
 * desde los value objects del dominio.
 */
public interface SpringDataUserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Page<UserEntity> findByUsernameContainingOrEmailContaining(
            String username, String email, Pageable pageable);

    @Query("select count(u) from UserEntity u join u.roles r where r = :role and u.enabled = true")
    long countEnabledUsersWithRole(@Param("role") Role role);
}
