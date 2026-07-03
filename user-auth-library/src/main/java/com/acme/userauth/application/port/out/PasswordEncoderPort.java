package com.acme.userauth.application.port.out;

import com.acme.userauth.domain.vo.PasswordHash;

/**
 * Puerto de salida para la codificación y verificación de contraseñas.
 * <p>
 * Abstrae el algoritmo concreto (BCrypt, Argon2, SCrypt…) para que la capa de
 * aplicación no dependa de Spring Security ni de ninguna librería de
 * criptografía. Cambiar de algoritmo es implementar un adaptador nuevo.
 */
public interface PasswordEncoderPort {

    /**
     * Codifica una contraseña en claro.
     *
     * @param rawPassword contraseña en claro; el llamante es responsable de
     *                    no retenerla más de lo imprescindible
     * @return hash resultante como value object del dominio
     */
    PasswordHash encode(CharSequence rawPassword);

    /**
     * Verifica si una contraseña en claro se corresponde con un hash.
     *
     * @param rawPassword contraseña en claro a comprobar
     * @param hash        hash almacenado contra el que comparar
     * @return {@code true} si coinciden
     */
    boolean matches(CharSequence rawPassword, PasswordHash hash);
}
