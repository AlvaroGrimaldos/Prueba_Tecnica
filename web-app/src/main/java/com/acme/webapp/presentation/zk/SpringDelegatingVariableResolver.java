package com.acme.webapp.presentation.zk;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.zkoss.xel.VariableResolver;
import org.zkoss.xel.XelException;
import org.zkoss.zk.ui.Executions;

import jakarta.servlet.ServletRequest;

/**
 * Resolutor de variables ZK que delega en el {@code ApplicationContext} de
 * Spring: permite inyectar beans de Spring en los ViewModels mediante
 * {@code @WireVariable("nombreDelBean")}.
 * <p>
 * Sustituye al histórico {@code org.zkoss.zkplus.spring.DelegatingVariableResolver},
 * retirado en ZK 10. El contexto se localiza en cada resolución a través del
 * {@code ServletContext} de la ejecución actual, así que la clase no guarda
 * estado y ZK puede instanciarla libremente (requiere constructor por defecto).
 * <p>
 * Uso en un ViewModel:
 * <pre>{@code
 * @VariableResolver(SpringDelegatingVariableResolver.class)
 * public class MiViewModel { @WireVariable("userService") ... }
 * }</pre>
 */
public class SpringDelegatingVariableResolver implements VariableResolver {

    @Override
    public Object resolveVariable(String name) throws XelException {
        ApplicationContext context = currentApplicationContext();
        if (context != null && context.containsBean(name)) {
            return context.getBean(name);
        }
        return null;
    }

    private ApplicationContext currentApplicationContext() {
        var execution = Executions.getCurrent();
        if (execution == null) {
            return null;
        }
        var request = (ServletRequest) execution.getNativeRequest();
        return WebApplicationContextUtils.getWebApplicationContext(request.getServletContext());
    }

    /**
     * ZK deduplica los resolutores registrados por página comparándolos con
     * {@code equals}; al ser una clase sin estado, dos instancias son
     * siempre equivalentes.
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof SpringDelegatingVariableResolver;
    }

    @Override
    public int hashCode() {
        return SpringDelegatingVariableResolver.class.hashCode();
    }
}
