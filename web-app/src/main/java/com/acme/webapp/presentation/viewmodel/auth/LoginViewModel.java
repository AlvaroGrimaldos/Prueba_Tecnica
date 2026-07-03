package com.acme.webapp.presentation.viewmodel.auth;

import org.zkoss.bind.annotation.Command;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.Clients;
import com.acme.webapp.presentation.zk.SpringDelegatingVariableResolver;

import com.acme.webapp.infrastructure.security.ZkLoginService;

/**
 * ViewModel de la página de login ({@code login.zul}).
 * <p>
 * Delegación pura: credenciales → {@link ZkLoginService}. El mensaje de error
 * es genérico a propósito — no distingue "usuario inexistente" de "contraseña
 * incorrecta" para no permitir enumerar cuentas.
 */
@VariableResolver(SpringDelegatingVariableResolver.class)
public class LoginViewModel {

    @WireVariable("zkLoginService")
    private ZkLoginService zkLoginService;

    private String username = "";
    private String password = "";

    /** Autentica y redirige a la home; si falla, informa sin dar pistas. */
    @Command
    public void login() {
        var result = zkLoginService.login(username, password);
        if (result.authenticated()) {
            Executions.sendRedirect("/");
        } else {
            Clients.showNotification("Credenciales inválidas o cuenta deshabilitada",
                    Clients.NOTIFICATION_TYPE_ERROR, null, "middle_center", 3500);
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
