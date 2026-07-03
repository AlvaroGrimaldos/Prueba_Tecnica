package com.acme.webapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de arranque de la aplicación web ZK.
 * <p>
 * Los beans de negocio (casos de uso, adaptadores de persistencia) llegan de
 * la autoconfiguración de {@code user-auth-library}; los ViewModels de ZK no
 * son beans de Spring —los instancia el binder de ZK por página— y obtienen
 * sus dependencias a través de {@code DelegatingVariableResolver}.
 */
@SpringBootApplication
public class WebAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebAppApplication.class, args);
    }
}
