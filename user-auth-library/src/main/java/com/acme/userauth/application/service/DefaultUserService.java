package com.acme.userauth.application.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.acme.userauth.application.dto.ChangePasswordCommand;
import com.acme.userauth.application.dto.PageResult;
import com.acme.userauth.application.dto.RegisterUserCommand;
import com.acme.userauth.application.dto.ResetPasswordCommand;
import com.acme.userauth.application.dto.UserResponse;
import com.acme.userauth.application.port.in.UserService;
import com.acme.userauth.application.port.out.PasswordEncoderPort;
import com.acme.userauth.application.port.out.UserRepositoryPort;
import com.acme.userauth.domain.exception.AuthenticationException;
import com.acme.userauth.domain.exception.UserNotFoundException;
import com.acme.userauth.domain.exception.UserAlreadyExistsException;
import com.acme.userauth.domain.exception.UserOperationNotAllowedException;
import com.acme.userauth.domain.model.Role;
import com.acme.userauth.domain.model.User;
import com.acme.userauth.domain.vo.Email;
import com.acme.userauth.domain.vo.UserId;
import com.acme.userauth.domain.vo.Username;

/**
 * Implementación por defecto del puerto {@link UserService}.
 * <p>
 * Orquesta el dominio y los puertos de salida: convierte los DTOs de frontera
 * en value objects (donde vuelven a validarse las invariantes), aplica las
 * reglas de unicidad, delega la codificación de contraseñas en
 * {@link PasswordEncoderPort} y traduce el agregado a {@link UserResponse}
 * hacia fuera. No contiene reglas de negocio propias del agregado: esas viven
 * en {@link User}.
 * <p>
 * No lleva {@code @Service}: se registra como bean en la autoconfiguración de
 * la librería para que el consumidor controle su activación. {@code @Validated}
 * habilita la validación declarativa de los comandos anotados con
 * {@code @Valid} en la interfaz.
 */
@Validated
public class DefaultUserService implements UserService {

    private final UserRepositoryPort users;
    private final PasswordEncoderPort passwordEncoder;

    public DefaultUserService(UserRepositoryPort users, PasswordEncoderPort passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterUserCommand command) {
        var username = new Username(command.username());
        var email = new Email(command.email());

        if (users.existsByUsername(username)) {
            throw UserAlreadyExistsException.byUsername(username.value());
        }
        if (users.existsByEmail(email)) {
            throw UserAlreadyExistsException.byEmail(email.value());
        }

        var user = User.register(
                username,
                email,
                passwordEncoder.encode(command.rawPassword()),
                command.roles());

        return UserResponse.from(users.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(UUID id) {
        return UserResponse.from(requireUser(id));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findByUsername(String username) {
        return users.findByUsername(new Username(username))
                .map(UserResponse::from)
                .orElseThrow(() -> UserNotFoundException.byUsername(username));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return users.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<UserResponse> search(String filter, int page, int size) {
        var normalized = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        return users.search(normalized, Math.max(0, page), Math.max(1, size))
                .map(UserResponse::from);
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordCommand command) {
        var user = requireUser(userId);
        if (!passwordEncoder.matches(command.currentPassword(), user.passwordHash())) {
            throw new AuthenticationException("current password does not match");
        }
        user.changePassword(passwordEncoder.encode(command.newPassword()));
        users.save(user);
    }

    @Override
    @Transactional
    public void resetPassword(UUID userId, ResetPasswordCommand command) {
        var user = requireUser(userId);
        user.changePassword(passwordEncoder.encode(command.newPassword()));
        users.save(user);
    }

    @Override
    @Transactional
    public void disable(UUID userId) {
        var user = requireUser(userId);
        ensureNotLastEnabledAdmin(user);
        user.disable();
        users.save(user);
    }

    @Override
    @Transactional
    public void enable(UUID userId) {
        var user = requireUser(userId);
        user.enable();
        users.save(user);
    }

    @Override
    @Transactional
    public void delete(UUID userId) {
        var user = requireUser(userId);
        ensureNotLastEnabledAdmin(user);
        users.deleteById(new UserId(userId));
    }

    private User requireUser(UUID id) {
        return users.findById(new UserId(id))
                .orElseThrow(() -> UserNotFoundException.byId(id));
    }

    /**
     * Regla de protección del sistema: el último administrador habilitado no
     * puede deshabilitarse ni eliminarse; de lo contrario la aplicación
     * quedaría sin nadie capaz de administrarla.
     */
    private void ensureNotLastEnabledAdmin(User user) {
        if (user.enabled()
                && user.hasRole(Role.ADMIN)
                && users.countEnabledUsersWithRole(Role.ADMIN) <= 1) {
            throw UserOperationNotAllowedException.lastAdministrator(user.username().value());
        }
    }
}
