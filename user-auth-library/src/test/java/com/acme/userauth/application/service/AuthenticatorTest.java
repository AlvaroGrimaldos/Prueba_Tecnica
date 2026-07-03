package com.acme.userauth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.acme.userauth.application.dto.AuthenticationRequest;
import com.acme.userauth.application.dto.AuthenticationResult;
import com.acme.userauth.domain.exception.AuthenticationException;

/**
 * Verifica el contrato del método plantilla: nunca devuelve null, nunca
 * propaga {@link AuthenticationException} y los ganchos no pueden alterar el
 * desenlace.
 */
class AuthenticatorTest {

    private record DummyRequest(String principal) implements AuthenticationRequest {
    }

    private static final class TestAuthenticator extends Authenticator<DummyRequest> {

        private final Function<DummyRequest, AuthenticationResult> behavior;
        private RuntimeException onSuccessException;
        private boolean successNotified;
        private boolean failureNotified;
        private AuthenticationException reportedFailure;

        private TestAuthenticator(Function<DummyRequest, AuthenticationResult> behavior) {
            this.behavior = behavior;
        }

        @Override
        public boolean supports(Class<? extends AuthenticationRequest> requestType) {
            return DummyRequest.class.isAssignableFrom(requestType);
        }

        @Override
        protected AuthenticationResult doAuthenticate(DummyRequest request) {
            return behavior.apply(request);
        }

        @Override
        protected void onSuccess(DummyRequest request, AuthenticationResult result) {
            successNotified = true;
            if (onSuccessException != null) {
                throw onSuccessException;
            }
        }

        @Override
        protected void onFailure(DummyRequest request, AuthenticationException failure) {
            failureNotified = true;
            reportedFailure = failure;
        }
    }

    private static final DummyRequest REQUEST = new DummyRequest("andres");

    private static AuthenticationResult ok() {
        return AuthenticationResult.success("andres", Set.of("USER"));
    }

    @Test
    void returnsSuccessAndNotifiesHook() {
        var authenticator = new TestAuthenticator(r -> ok());

        var result = authenticator.authenticate(REQUEST);

        assertThat(result.authenticated()).isTrue();
        assertThat(authenticator.successNotified).isTrue();
        assertThat(authenticator.failureNotified).isFalse();
    }

    @Test
    void translatesAuthenticationExceptionIntoFailureResult() {
        var authenticator = new TestAuthenticator(r -> {
            throw new AuthenticationException("bad credentials");
        });

        var result = authenticator.authenticate(REQUEST);

        assertThat(result.authenticated()).isFalse();
        assertThat(result.failureReason()).isEqualTo("bad credentials");
        assertThat(authenticator.failureNotified).isTrue();
        assertThat(authenticator.reportedFailure).isNotNull();
    }

    @Test
    void providesDefaultReasonWhenExceptionHasNoMessage() {
        var authenticator = new TestAuthenticator(r -> {
            throw new AuthenticationException(null);
        });

        var result = authenticator.authenticate(REQUEST);

        assertThat(result.authenticated()).isFalse();
        assertThat(result.failureReason()).isEqualTo("authentication failed");
    }

    @Test
    void successHookFailureDoesNotAlterOutcome() {
        var authenticator = new TestAuthenticator(r -> ok());
        authenticator.onSuccessException = new IllegalStateException("audit down");

        var result = authenticator.authenticate(REQUEST);

        assertThat(result.authenticated()).isTrue();
    }

    @Test
    void rejectsNullRequest() {
        var authenticator = new TestAuthenticator(r -> ok());
        assertThatNullPointerException().isThrownBy(() -> authenticator.authenticate(null));
    }
}
