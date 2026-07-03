package com.acme.webapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

/**
 * Configuración de Spring Security para la aplicación ZK.
 * <p>
 * La autenticación no usa el form-login de Spring Security: la realiza
 * programáticamente {@code ZkLoginService} a través del
 * {@code PasswordAuthenticator} de user-auth-library, y aquí solo se
 * gobiernan la autorización de URLs y el punto de entrada.
 * <p>
 * Detalle importante del empaquetado jar de ZK: las vistas .zul se sirven
 * bajo {@code /zkau/web/...} (a donde hacen forward las URLs amigables como
 * {@code /} o {@code /login}). El forward interno no pasa por el filtro de
 * seguridad, pero esas rutas también son accesibles por petición directa,
 * así que se protegen explícitamente: sin la regla sobre
 * {@code /zkau/web/zul/**}, un anónimo podría abrir el CRUD escribiendo la
 * URL interna a mano.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** URL pública de login (view controller definido en {@link WebConfig}). */
    public static final String LOGIN_PAGE = "/login";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // El motor AU de ZK enruta la interacción de los desktops por
                // /zkau con su propio control por identificadores de desktop;
                // el CSRF de Spring Security aplica al resto (no hay forms HTML).
                .csrf(csrf -> csrf.ignoringRequestMatchers("/zkau/**"))
                .authorizeHttpRequests(auth -> auth
                        // Página de login, también por su ruta interna directa
                        .requestMatchers(LOGIN_PAGE, "/zkau/web/zul/auth/**").permitAll()
                        // Ningún otro .zul de la aplicación sin rol ADMIN
                        .requestMatchers("/zkau/web/zul/**").hasRole("ADMIN")
                        // Motor AU + recursos estáticos de ZK (js/css bajo /zkau/web/js…)
                        .requestMatchers("/zkau/**", "/css/**", "/favicon.ico", "/error").permitAll()
                        // Resto (p. ej. "/" → homepage): autenticado
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint(LOGIN_PAGE)));
        return http.build();
    }
}
