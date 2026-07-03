package com.acme.webapp.presentation.viewmodel.user;

import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.ContextParam;
import org.zkoss.bind.annotation.ContextType;
import org.zkoss.bind.annotation.Init;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.Clients;
import com.acme.webapp.presentation.zk.SpringDelegatingVariableResolver;

import com.acme.userauth.application.dto.ChangePasswordCommand;
import com.acme.userauth.application.port.in.UserService;
import com.acme.userauth.domain.exception.AuthenticationException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

/**
 * ViewModel del diálogo "cambiar mi contraseña"
 * ({@code change-password.zul}).
 * <p>
 * Opera siempre sobre el usuario autenticado en la sesión (resuelto desde el
 * {@code SecurityContext}), nunca sobre un usuario arbitrario: por eso exige
 * la contraseña actual. Para actuar sobre otras cuentas existe el
 * restablecimiento administrativo ({@link ResetPasswordViewModel}).
 */
@VariableResolver(SpringDelegatingVariableResolver.class)
public class ChangePasswordViewModel {

    @WireVariable("userService")
    private UserService userService;

    private UUID userId;
    private String username;

    private String currentPassword = "";
    private String newPassword = "";
    private String confirmPassword = "";

    private Component view;

    @Init
    public void init(@ContextParam(ContextType.VIEW) Component view) {
        this.view = view;
        this.username = SecurityContextHolder.getContext().getAuthentication().getName();
        this.userId = userService.findByUsername(username).id();
    }

    /** Aplica el cambio de contraseña; mantiene el diálogo abierto si falla. */
    @Command
    public void save() {
        if (!newPassword.equals(confirmPassword)) {
            Clients.showNotification("La confirmación no coincide con la nueva contraseña",
                    Clients.NOTIFICATION_TYPE_WARNING, null, "middle_center", 3000);
            return;
        }
        try {
            userService.changePassword(userId,
                    new ChangePasswordCommand(currentPassword, newPassword));
            Clients.showNotification("Contraseña actualizada",
                    Clients.NOTIFICATION_TYPE_INFO, null, "top_center", 2500);
            close();
        } catch (AuthenticationException ex) {
            Clients.showNotification("La contraseña actual no es correcta",
                    Clients.NOTIFICATION_TYPE_ERROR, null, "middle_center", 4000);
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

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
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
