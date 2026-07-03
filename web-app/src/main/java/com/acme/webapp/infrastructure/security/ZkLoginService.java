package com.acme.webapp.infrastructure.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.zkoss.zk.ui.Executions;

import com.acme.userauth.application.dto.AuthenticationResult;
import com.acme.userauth.application.dto.PasswordAuthenticationRequest;
import com.acme.userauth.application.service.PasswordAuthenticator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Puente entre la autenticación de user-auth-library y la sesión de Spring
 * Security, pensado para invocarse desde un evento ZK (requiere una
 * {@code Execution} activa).
 * <p>
 * Responsabilidad única: traducir un {@link AuthenticationResult} exitoso en
 * un {@code SecurityContext} persistido en la sesión HTTP. No decide reglas
 * de autenticación (eso es del {@link PasswordAuthenticator}) ni de
 * autorización (eso es de {@code SecurityConfig}).
 */
@Service
public class ZkLoginService {

    private final PasswordAuthenticator passwordAuthenticator;
    private final SecurityContextRepository contextRepository =
            new HttpSessionSecurityContextRepository();

    public ZkLoginService(PasswordAuthenticator passwordAuthenticator) {
        this.passwordAuthenticator = passwordAuthenticator;
    }

    /**
     * Autentica las credenciales y, si son válidas, establece la sesión de
     * Spring Security.
     *
     * @return el resultado de la autenticación (también en caso de fallo,
     *         para que la vista muestre el desenlace sin manejar excepciones)
     */
    public AuthenticationResult login(String username, String rawPassword) {
        var result = passwordAuthenticator.authenticate(
                new PasswordAuthenticationRequest(username, rawPassword));
        if (!result.authenticated()) {
            return result;
        }

        var execution = Executions.getCurrent();
        var request = (HttpServletRequest) execution.getNativeRequest();
        var response = (HttpServletResponse) execution.getNativeResponse();

        // Protección contra fijación de sesión: nueva id tras autenticar
        request.changeSessionId();

        var authorities = result.roles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        var authentication = UsernamePasswordAuthenticationToken
                .authenticated(result.principal(), null, authorities);

        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);

        return result;
    }

    /** Cierra la sesión: limpia el contexto e invalida la sesión ZK/HTTP. */
    public void logout() {
        SecurityContextHolder.clearContext();
        Executions.getCurrent().getDesktop().getSession().invalidate();
    }
}
