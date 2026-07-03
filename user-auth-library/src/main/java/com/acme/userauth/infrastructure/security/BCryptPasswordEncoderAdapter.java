package com.acme.userauth.infrastructure.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.acme.userauth.application.port.out.PasswordEncoderPort;
import com.acme.userauth.domain.vo.PasswordHash;

/**
 * Adaptador BCrypt del puerto {@link PasswordEncoderPort}.
 * <p>
 * Usa {@code spring-security-crypto} (solo el módulo criptográfico, sin la
 * cadena de filtros web). Cambiar a Argon2 u otro algoritmo es crear otro
 * adaptador y registrarlo como bean; ninguna otra capa se entera.
 */
public class BCryptPasswordEncoderAdapter implements PasswordEncoderPort {

    private final BCryptPasswordEncoder encoder;

    /**
     * @param strength factor de coste de BCrypt (2^strength iteraciones);
     *                 12 es un valor razonable actual — configurable desde
     *                 la autoconfiguración
     */
    public BCryptPasswordEncoderAdapter(int strength) {
        this.encoder = new BCryptPasswordEncoder(strength);
    }

    @Override
    public PasswordHash encode(CharSequence rawPassword) {
        return new PasswordHash(encoder.encode(rawPassword));
    }

    @Override
    public boolean matches(CharSequence rawPassword, PasswordHash hash) {
        return encoder.matches(rawPassword, hash.value());
    }
}
