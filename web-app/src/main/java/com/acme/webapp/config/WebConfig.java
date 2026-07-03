package com.acme.webapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * URLs amigables para las vistas ZK.
 * <p>
 * En empaquetado jar, los .zul del classpath se sirven internamente bajo
 * {@code /zkau/web/...}; estos view controllers dan URLs limpias que el
 * resolutor de vistas de zkspringboot traduce a un forward interno
 * ({@code /login} → forward a {@code /zkau/web/zul/auth/login.zul}).
 * La homepage ({@code /} → listado) la registra el propio starter a partir
 * de la propiedad {@code zk.homepage}.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * URL pública de login. El nombre de vista es relativo: el resolutor del
     * starter ya antepone {@code /zkau/web/} (con barra final), así que un
     * nombre con barra inicial produciría {@code //} y el firewall de Spring
     * Security rechazaría la petición.
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("zul/auth/login");
    }
}
