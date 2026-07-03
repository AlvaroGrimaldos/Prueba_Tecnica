package com.acme.userauth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.acme.userauth.application.dto.ChangePasswordCommand;
import com.acme.userauth.application.dto.RegisterUserCommand;
import com.acme.userauth.application.dto.ResetPasswordCommand;
import com.acme.userauth.application.port.out.PasswordEncoderPort;
import com.acme.userauth.application.port.out.UserRepositoryPort;
import com.acme.userauth.domain.exception.AuthenticationException;
import com.acme.userauth.domain.exception.UserAlreadyExistsException;
import com.acme.userauth.domain.exception.UserNotFoundException;
import com.acme.userauth.domain.exception.UserOperationNotAllowedException;
import com.acme.userauth.domain.model.Role;
import com.acme.userauth.domain.model.User;
import com.acme.userauth.domain.vo.Email;
import com.acme.userauth.domain.vo.PasswordHash;
import com.acme.userauth.domain.vo.UserId;
import com.acme.userauth.domain.vo.Username;

@ExtendWith(MockitoExtension.class)
class DefaultUserServiceTest {

    @Mock
    private UserRepositoryPort users;

    @Mock
    private PasswordEncoderPort encoder;

    @InjectMocks
    private DefaultUserService service;

    private static User adminUser(boolean enabled) {
        return User.reconstitute(UserId.newId(), new Username("admin"),
                new Email("admin@acme.com"), new PasswordHash("$hash$"),
                Set.of(Role.ADMIN), enabled, java.time.Instant.now());
    }

    /* ------------------------------ register ------------------------------ */

    @Test
    void registerEncodesPasswordAndPersists() {
        when(users.existsByUsername(any())).thenReturn(false);
        when(users.existsByEmail(any())).thenReturn(false);
        when(encoder.encode("Secreta1")).thenReturn(new PasswordHash("$encoded$"));
        when(users.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.register(new RegisterUserCommand(
                "Andres", "Andres@Acme.com", "Secreta1", Set.of(Role.USER)));

        assertThat(response.username()).isEqualTo("andres");
        assertThat(response.email()).isEqualTo("andres@acme.com");
        assertThat(response.enabled()).isTrue();
        verify(users).save(any(User.class));
    }

    @Test
    void registerRejectsDuplicateUsernameWithoutPersisting() {
        when(users.existsByUsername(new Username("andres"))).thenReturn(true);

        assertThatExceptionOfType(UserAlreadyExistsException.class)
                .isThrownBy(() -> service.register(new RegisterUserCommand(
                        "andres", "a@acme.com", "Secreta1", Set.of(Role.USER))));

        verify(users, never()).save(any());
    }

    @Test
    void registerRejectsDuplicateEmailWithoutPersisting() {
        when(users.existsByUsername(any())).thenReturn(false);
        when(users.existsByEmail(new Email("a@acme.com"))).thenReturn(true);

        assertThatExceptionOfType(UserAlreadyExistsException.class)
                .isThrownBy(() -> service.register(new RegisterUserCommand(
                        "andres", "a@acme.com", "Secreta1", Set.of(Role.USER))));

        verify(users, never()).save(any());
    }

    /* -------------------------- change / reset ---------------------------- */

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        var user = adminUser(true);
        when(users.findById(user.id())).thenReturn(Optional.of(user));
        when(encoder.matches("mala", user.passwordHash())).thenReturn(false);

        assertThatExceptionOfType(AuthenticationException.class)
                .isThrownBy(() -> service.changePassword(user.id().value(),
                        new ChangePasswordCommand("mala", "Nueva1234")));

        verify(users, never()).save(any());
    }

    @Test
    void changePasswordEncodesAndPersistsNewHash() {
        var user = adminUser(true);
        when(users.findById(user.id())).thenReturn(Optional.of(user));
        when(encoder.matches("Actual1", user.passwordHash())).thenReturn(true);
        when(encoder.encode("Nueva1234")).thenReturn(new PasswordHash("$new$"));
        when(users.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.changePassword(user.id().value(), new ChangePasswordCommand("Actual1", "Nueva1234"));

        assertThat(user.passwordHash().value()).isEqualTo("$new$");
        verify(users).save(user);
    }

    @Test
    void resetPasswordDoesNotRequireCurrentPassword() {
        var user = adminUser(true);
        when(users.findById(user.id())).thenReturn(Optional.of(user));
        when(encoder.encode("Nueva1234")).thenReturn(new PasswordHash("$new$"));
        when(users.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.resetPassword(user.id().value(), new ResetPasswordCommand("Nueva1234"));

        assertThat(user.passwordHash().value()).isEqualTo("$new$");
        verify(encoder, never()).matches(any(), any());
    }

    /* ------------------------ last-admin protection ----------------------- */

    @Test
    void disableRejectsLastEnabledAdministrator() {
        var admin = adminUser(true);
        when(users.findById(admin.id())).thenReturn(Optional.of(admin));
        when(users.countEnabledUsersWithRole(Role.ADMIN)).thenReturn(1L);

        assertThatExceptionOfType(UserOperationNotAllowedException.class)
                .isThrownBy(() -> service.disable(admin.id().value()));

        verify(users, never()).save(any());
    }

    @Test
    void disableAllowsAdminWhenAnotherOneRemains() {
        var admin = adminUser(true);
        when(users.findById(admin.id())).thenReturn(Optional.of(admin));
        when(users.countEnabledUsersWithRole(Role.ADMIN)).thenReturn(2L);
        when(users.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.disable(admin.id().value());

        assertThat(admin.enabled()).isFalse();
        verify(users).save(admin);
    }

    @Test
    void deleteRejectsLastEnabledAdministrator() {
        var admin = adminUser(true);
        when(users.findById(admin.id())).thenReturn(Optional.of(admin));
        when(users.countEnabledUsersWithRole(Role.ADMIN)).thenReturn(1L);

        assertThatExceptionOfType(UserOperationNotAllowedException.class)
                .isThrownBy(() -> service.delete(admin.id().value()));

        verify(users, never()).deleteById(any());
    }

    /* -------------------------------- misc -------------------------------- */

    @Test
    void findByIdTranslatesMissingUser() {
        var id = UUID.randomUUID();
        when(users.findById(new UserId(id))).thenReturn(Optional.empty());

        assertThatExceptionOfType(UserNotFoundException.class)
                .isThrownBy(() -> service.findById(id));
    }
}
