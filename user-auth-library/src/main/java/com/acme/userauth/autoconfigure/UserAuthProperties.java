package com.acme.userauth.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de configuración de la librería ({@code userauth.*}).
 * <p>
 * Ejemplo en {@code application.yml}:
 * <pre>{@code
 * userauth:
 *   bcrypt-strength: 12
 * }</pre>
 */
@ConfigurationProperties(prefix = "userauth")
public class UserAuthProperties {

    /** Factor de coste de BCrypt (2^n iteraciones). */
    private int bcryptStrength = 12;

    public int getBcryptStrength() {
        return bcryptStrength;
    }

    public void setBcryptStrength(int bcryptStrength) {
        this.bcryptStrength = bcryptStrength;
    }
}
