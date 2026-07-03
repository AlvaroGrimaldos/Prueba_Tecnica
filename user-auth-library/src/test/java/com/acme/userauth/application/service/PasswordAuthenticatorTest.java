package com.acme.userauth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.acme.userauth.application.dto.PasswordAuthenticationRequest;
import com.acme.userauth.application.port.out.PasswordEncoderPort;
import com.acme.userauth.application.port.out.UserRepositoryPort;
import com.acme.userauth.domain.model.Role;
import com.acme.userauth.domain.model.User;
import com.acme.userauth.domain.vo.Email;
import com.acme.userauth.domain.vo.PasswordHash;
import com.acme.userauth.domain.vo.Username;

@ExtendWith(MockitoExtension.class)
class PasswordAuthenticatorTest {

    @Mock
    private UserRepositoryPort users;

    @Mock
    private PasswordEncoderPort encoder;

    @InjectMocks
    private PasswordAuthenticator authenticator;

    private static User user(boolean enabled) {
        return User.reconstitute(
                com.acme.userauth.domain.vo.UserId.newId(),
                new Username("andres"),
                new Email("andres@acme.com"),
                new PasswordHash("$hash$"),
                Set.of(Role.ADMIN, Role.USER),
                enabled,
                java.time.Instant.now());
    }

    @Test
    void authenticatesEnabledUserWithMatchingPassword() {
        when(users.findByUsername(new Username("andres"))).thenReturn(Optional.of(user(true)));
        when(encoder.matches(any(), any())).thenReturn(true);

        var result = authenticator.authenticate(new PasswordAuthenticationRequest("andres", "Secreta1"));

        assertThat(result.authenticated()).isTrue();
        assertThat(result.principal()).isEqualTo("andres");
        assertThat(result.roles()).containsExactlyInAnyOrder("ADMIN", "USER");
    }

    @Test
    void failsWithGenericReasonForUnknownUser() {
        when(users.findByUsername(any())).thenReturn(Optional.empty());

        var result = authenticator.authenticate(new PasswordAuthenticationRequest("nadie", "Secreta1"));

        assertThat(result.authenticated()).isFalse();
        assertThat(result.failureReason()).isEqualTo(PasswordAuthenticator.GENERIC_FAILURE);
    }

    @Test
    void failsWithGenericReasonForWrongPassword() {
        when(users.findByUsername(any())).thenReturn(Optional.of(user(true)));
        when(encoder.matches(any(), any())).thenReturn(false);

        var result = authenticator.authenticate(new PasswordAuthenticationRequest("andres", "mala"));

        assertThat(result.authenticated()).isFalse();
        assertThat(result.failureReason()).isEqualTo(PasswordAuthenticator.GENERIC_FAILURE);
    }

    @Test
    void revealsDisabledAccountOnlyAfterCorrectPassword() {
        when(users.findByUsername(any())).thenReturn(Optional.of(user(false)));
        when(encoder.matches(any(), any())).thenReturn(true);

        var result = authenticator.authenticate(new PasswordAuthenticationRequest("andres", "Secreta1"));

        assertThat(result.authenticated()).isFalse();
        assertThat(result.failureReason()).isEqualTo(PasswordAuthenticator.ACCOUNT_DISABLED);
    }

    @Test
    void failsWithGenericReasonForMalformedUsernameWithoutTouchingTheRepository() {
        var result = authenticator.authenticate(new PasswordAuthenticationRequest("x", "Secreta1"));

        assertThat(result.authenticated()).isFalse();
        assertThat(result.failureReason()).isEqualTo(PasswordAuthenticator.GENERIC_FAILURE);
    }

    @Test
    void failsOnBlankCredentials() {
        var result = authenticator.authenticate(new PasswordAuthenticationRequest("  ", ""));

        assertThat(result.authenticated()).isFalse();
        assertThat(result.failureReason()).isEqualTo(PasswordAuthenticator.GENERIC_FAILURE);
    }
}
