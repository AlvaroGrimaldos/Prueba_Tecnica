package com.acme.userauth.application.service;

import java.util.stream.Collectors;

import com.acme.userauth.application.dto.AuthenticationRequest;
import com.acme.userauth.application.dto.AuthenticationResult;
import com.acme.userauth.application.dto.PasswordAuthenticationRequest;
import com.acme.userauth.application.port.out.PasswordEncoderPort;
import com.acme.userauth.application.port.out.UserRepositoryPort;
import com.acme.userauth.domain.exception.AuthenticationException;
import com.acme.userauth.domain.vo.Username;

/**
 * Mecanismo de autenticación por usuario/contraseña contra el almacén de
 * usuarios de la librería.
 * <p>
 * Reglas aplicadas, en orden: el usuario debe existir, la contraseña debe
 * coincidir y la cuenta debe estar habilitada.
 * <p>
 * <b>Seguridad — mensajes genéricos:</b> todos los fallos atribuibles al
 * atacante (usuario inexistente, contraseña errónea, entrada malformada)
 * devuelven el mismo motivo, para no permitir enumerar cuentas existentes.
 * El estado "deshabilitada" solo se revela tras verificar la contraseña,
 * es decir, a quien ya posee credenciales válidas.
 */
public class PasswordAuthenticator extends Authenticator<PasswordAuthenticationRequest> {

    /** Motivo único para todo fallo no distinguible por diseño. */
    public static final String GENERIC_FAILURE = "invalid credentials";

    /** Motivo para cuenta deshabilitada (solo tras contraseña correcta). */
    public static final String ACCOUNT_DISABLED = "account is disabled";

    private final UserRepositoryPort users;
    private final PasswordEncoderPort passwordEncoder;

    public PasswordAuthenticator(UserRepositoryPort users, PasswordEncoderPort passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean supports(Class<? extends AuthenticationRequest> requestType) {
        return PasswordAuthenticationRequest.class.isAssignableFrom(requestType);
    }

    @Override
    protected void validate(PasswordAuthenticationRequest request) {
        if (request.principal() == null || request.principal().isBlank()
                || request.rawPassword() == null || request.rawPassword().isBlank()) {
            throw new AuthenticationException(GENERIC_FAILURE);
        }
    }

    @Override
    protected AuthenticationResult doAuthenticate(PasswordAuthenticationRequest request) {
        Username username;
        try {
            username = new Username(request.principal());
        } catch (IllegalArgumentException ex) {
            // Entrada malformada == credenciales inválidas: mismo mensaje
            throw new AuthenticationException(GENERIC_FAILURE, ex);
        }

        var user = users.findByUsername(username)
                .orElseThrow(() -> new AuthenticationException(GENERIC_FAILURE));

        if (!passwordEncoder.matches(request.rawPassword(), user.passwordHash())) {
            throw new AuthenticationException(GENERIC_FAILURE);
        }
        if (!user.enabled()) {
            throw new AuthenticationException(ACCOUNT_DISABLED);
        }

        return AuthenticationResult.success(
                user.username().value(),
                user.roles().stream().map(Enum::name).collect(Collectors.toUnmodifiableSet()));
    }
}
