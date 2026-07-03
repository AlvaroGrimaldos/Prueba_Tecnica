package com.acme.userauth.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.acme.userauth.domain.vo.Email;
import com.acme.userauth.domain.vo.PasswordHash;
import com.acme.userauth.domain.vo.Username;

class UserTest {

    private User newUser() {
        return User.register(
                new Username("andres"),
                new Email("andres@acme.com"),
                new PasswordHash("$hash$"),
                Set.of(Role.USER));
    }

    @Test
    void registerCreatesEnabledUserWithIdentityAndTimestamp() {
        var user = newUser();

        assertThat(user.id()).isNotNull();
        assertThat(user.enabled()).isTrue();
        assertThat(user.createdAt()).isNotNull();
        assertThat(user.hasRole(Role.USER)).isTrue();
        assertThat(user.hasRole(Role.ADMIN)).isFalse();
    }

    @Test
    void requiresAtLeastOneRole() {
        assertThatIllegalArgumentException().isThrownBy(() -> User.register(
                new Username("andres"),
                new Email("andres@acme.com"),
                new PasswordHash("$hash$"),
                Set.of()));
    }

    @Test
    void rolesViewIsImmutable() {
        var user = newUser();
        assertThatThrownBy(() -> user.roles().add(Role.ADMIN))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void changePasswordReplacesHash() {
        var user = newUser();
        user.changePassword(new PasswordHash("$new$"));
        assertThat(user.passwordHash().value()).isEqualTo("$new$");
    }

    @Test
    void disableAndEnableToggleTheAccount() {
        var user = newUser();
        user.disable();
        assertThat(user.enabled()).isFalse();
        user.enable();
        assertThat(user.enabled()).isTrue();
    }

    @Test
    void equalityIsByIdentityNotByAttributes() {
        var a = newUser();
        var b = User.reconstitute(a.id(), new Username("otro"), new Email("otro@acme.com"),
                new PasswordHash("$x$"), Set.of(Role.ADMIN), false, a.createdAt());

        assertThat(a).isEqualTo(b);
        assertThat(a).isNotEqualTo(newUser());
    }
}
