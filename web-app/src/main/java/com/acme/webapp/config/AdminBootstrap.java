package com.acme.webapp.config;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.acme.userauth.application.dto.RegisterUserCommand;
import com.acme.userauth.application.port.in.UserService;
import com.acme.userauth.domain.model.Role;

/**
 * Crea el administrador inicial si la base de datos está vacía.
 * <p>
 * Sin esto la aplicación sería inaccesible: el alta de usuarios exige rol
 * {@code ADMIN} y no habría con quién iniciar sesión. Las credenciales se
 * externalizan a variables de entorno; los valores por defecto son solo para
 * desarrollo local y se avisa por log de que deben cambiarse.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserService userService;
    private final String username;
    private final String email;
    private final String password;

    public AdminBootstrap(UserService userService,
                          @Value("${app.bootstrap-admin.username:admin}") String username,
                          @Value("${app.bootstrap-admin.email:admin@acme.local}") String email,
                          @Value("${app.bootstrap-admin.password:Admin1234}") String password) {
        this.userService = userService;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!userService.findAll().isEmpty()) {
            return;
        }
        userService.register(new RegisterUserCommand(username, email, password, Set.of(Role.ADMIN)));
        log.warn("Base de datos vacía: creado administrador inicial '{}'. "
                + "Cambia su contraseña inmediatamente.", username);
    }
}
