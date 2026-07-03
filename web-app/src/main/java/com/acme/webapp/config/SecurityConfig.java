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
 * Notas:
 * <ul>
 *   <li>{@code /zkau/**} (motor AU de ZK) se permite y se excluye de CSRF:
 *       toda la interacción de un desktop ZK viaja por ahí y ZK aplica su
 *       propio control por identificadores de desktop/página. La seguridad
 *       de URL protege la <em>creación</em> de cada página.</li>
 *   <li>Las páginas de administración ({@code /zul/user/**}) exigen rol
 *       {@code ADMIN}.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Página de login ZK; pública por definición. */
    public static final String LOGIN_PAGE = "/zul/auth/login.zul";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers("/zkau/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/zkau/**", "/css/**", "/favicon.ico", "/error").permitAll()
                        .requestMatchers(LOGIN_PAGE).permitAll()
                        .requestMatchers("/zul/user/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint(LOGIN_PAGE)));
        return http.build();
    }
}
