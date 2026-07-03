package com.acme.userauth.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.acme.userauth.application.port.in.UserService;
import com.acme.userauth.application.port.out.PasswordEncoderPort;
import com.acme.userauth.application.port.out.UserRepositoryPort;
import com.acme.userauth.application.service.DefaultUserService;
import com.acme.userauth.application.service.PasswordAuthenticator;
import com.acme.userauth.infrastructure.persistence.adapter.UserRepositoryAdapter;
import com.acme.userauth.infrastructure.persistence.entity.UserEntity;
import com.acme.userauth.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.acme.userauth.infrastructure.persistence.repository.SpringDataUserRepository;
import com.acme.userauth.infrastructure.security.BCryptPasswordEncoderAdapter;

/**
 * Autoconfiguración estilo "starter" de la librería.
 * <p>
 * Registra explícitamente los beans (nada de component scanning): el
 * consumidor solo añade la dependencia y, si necesita sustituir una pieza,
 * declara su propio bean — cada registro lleva
 * {@link ConditionalOnMissingBean @ConditionalOnMissingBean}.
 * <p>
 * Notas para el módulo consumidor:
 * <ul>
 *   <li>{@code @EnableJpaRepositories}/{@code @EntityScan} de esta clase
 *       desactivan el auto-escaneo por defecto de Spring Boot; si la
 *       aplicación define sus propios repositorios o entidades, debe declarar
 *       sus propias anotaciones apuntando a sus paquetes;</li>
 *   <li>las migraciones Flyway de la librería viven en
 *       {@code classpath:db/migration/userauth} — añadir esa ruta a
 *       {@code spring.flyway.locations}.</li>
 * </ul>
 */
@AutoConfiguration
@EnableConfigurationProperties(UserAuthProperties.class)
@EntityScan(basePackageClasses = UserEntity.class)
@EnableJpaRepositories(basePackageClasses = SpringDataUserRepository.class)
public class UserAuthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(PasswordEncoderPort.class)
    public PasswordEncoderPort passwordEncoderPort(UserAuthProperties properties) {
        return new BCryptPasswordEncoderAdapter(properties.getBcryptStrength());
    }

    @Bean
    @ConditionalOnMissingBean(UserPersistenceMapper.class)
    public UserPersistenceMapper userPersistenceMapper() {
        return new UserPersistenceMapper();
    }

    @Bean
    @ConditionalOnMissingBean(UserRepositoryPort.class)
    public UserRepositoryPort userRepositoryPort(SpringDataUserRepository repository,
                                                 UserPersistenceMapper mapper) {
        return new UserRepositoryAdapter(repository, mapper);
    }

    @Bean
    @ConditionalOnMissingBean(UserService.class)
    public UserService userService(UserRepositoryPort userRepositoryPort,
                                   PasswordEncoderPort passwordEncoderPort) {
        return new DefaultUserService(userRepositoryPort, passwordEncoderPort);
    }

    @Bean
    @ConditionalOnMissingBean(PasswordAuthenticator.class)
    public PasswordAuthenticator passwordAuthenticator(UserRepositoryPort userRepositoryPort,
                                                       PasswordEncoderPort passwordEncoderPort) {
        return new PasswordAuthenticator(userRepositoryPort, passwordEncoderPort);
    }
}
