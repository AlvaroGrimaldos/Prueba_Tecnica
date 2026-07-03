package com.acme.webapp.presentation.viewmodel.user;

import java.util.UUID;
import java.util.stream.Collectors;

import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.ContextParam;
import org.zkoss.bind.annotation.ContextType;
import org.zkoss.bind.annotation.ExecutionArgParam;
import org.zkoss.bind.annotation.Init;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.Clients;
import com.acme.webapp.presentation.zk.SpringDelegatingVariableResolver;

import com.acme.userauth.application.dto.ResetPasswordCommand;
import com.acme.userauth.application.port.in.UserService;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

/**
 * ViewModel del diálogo de restablecimiento administrativo de contraseña
 * ({@code reset-password.zul}).
 * <p>
 * A diferencia del cambio de contraseña propio, no pide la contraseña actual:
 * es una operación de administrador sobre la cuenta de otro usuario. El
 * acceso a esta pantalla ya está restringido al rol {@code ADMIN} por
 * Spring Security.
 */
@VariableResolver(SpringDelegatingVariableResolver.class)
public class ResetPasswordViewModel {

    @WireVariable("userService")
    private UserService userService;

    private UUID userId;
    private String username;

    private String newPassword = "";
    private String confirmPassword = "";

    private Component view;

    @Init
    public void init(@ContextParam(ContextType.VIEW) Component view,
                     @ExecutionArgParam("userId") UUID userId,
                     @ExecutionArgParam("username") String username) {
        this.view = view;
        this.userId = userId;
        this.username = username;
    }

    /** Aplica el restablecimiento; mantiene el diálogo abierto si falla. */
    @Command
    public void save() {
        if (!newPassword.equals(confirmPassword)) {
            Clients.showNotification("La confirmación no coincide con la nueva contraseña",
                    Clients.NOTIFICATION_TYPE_WARNING, null, "middle_center", 3000);
            return;
        }
        try {
            userService.resetPassword(userId, new ResetPasswordCommand(newPassword));
            Clients.showNotification("Contraseña restablecida para '%s'".formatted(username),
                    Clients.NOTIFICATION_TYPE_INFO, null, "top_center", 2500);
            close();
        } catch (ConstraintViolationException ex) {
            Clients.showNotification(ex.getConstraintViolations().stream()
                            .map(ConstraintViolation::getMessage)
                            .collect(Collectors.joining(". ")),
                    Clients.NOTIFICATION_TYPE_ERROR, null, "middle_center", 4000);
        }
    }

    /** Cierra el diálogo sin cambios. */
    @Command
    public void close() {
        view.detach();
    }

    public String getUsername() {
        return username;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
