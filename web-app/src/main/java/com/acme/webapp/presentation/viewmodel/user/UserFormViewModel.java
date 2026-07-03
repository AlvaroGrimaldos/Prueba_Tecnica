package com.acme.webapp.presentation.viewmodel.user;

import java.util.stream.Collectors;

import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.ContextParam;
import org.zkoss.bind.annotation.ContextType;
import org.zkoss.bind.annotation.Init;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.Clients;
import com.acme.webapp.presentation.zk.SpringDelegatingVariableResolver;

import com.acme.userauth.application.dto.RegisterUserCommand;
import com.acme.userauth.application.port.in.UserService;
import com.acme.userauth.domain.exception.UserAlreadyExistsException;
import com.acme.webapp.presentation.model.UserForm;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

/**
 * ViewModel del diálogo modal de alta de usuario ({@code user-form.zul}).
 * <p>
 * La vista se enlaza al {@link UserForm} mutable; al guardar, el formulario
 * se convierte en un {@link RegisterUserCommand} inmutable y se delega en el
 * puerto {@link UserService}. Los errores de negocio (duplicados, política de
 * contraseñas) se traducen a notificaciones de UI — la regla en sí nunca se
 * evalúa aquí.
 */
@VariableResolver(SpringDelegatingVariableResolver.class)
public class UserFormViewModel {

    @WireVariable
    private UserService userService;

    private final UserForm form = new UserForm();

    private Component view;

    @Init
    public void init(@ContextParam(ContextType.VIEW) Component view) {
        this.view = view;
    }

    /**
     * Registra el usuario. Si el backend rechaza la operación, el diálogo
     * permanece abierto mostrando el motivo, para que el usuario corrija.
     */
    @Command
    public void save() {
        if (!form.passwordsMatch()) {
            warn("Las contraseñas no coinciden");
            return;
        }
        if (form.selectedRoles().isEmpty()) {
            warn("Selecciona al menos un rol");
            return;
        }
        try {
            userService.register(new RegisterUserCommand(
                    form.getUsername(),
                    form.getEmail(),
                    form.getPassword(),
                    form.selectedRoles()));

            Clients.showNotification("Usuario creado correctamente",
                    Clients.NOTIFICATION_TYPE_INFO, null, "top_center", 2500);
            BindUtils.postGlobalCommand(null, null, UserListViewModel.GC_REFRESH_USERS, null);
            close();
        } catch (UserAlreadyExistsException | IllegalArgumentException ex) {
            error(ex.getMessage());
        } catch (ConstraintViolationException ex) {
            error(ex.getConstraintViolations().stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(". ")));
        }
    }

    /** Cierra el diálogo descartando los cambios. */
    @Command
    public void close() {
        view.detach();
    }

    public UserForm getForm() {
        return form;
    }

    private void warn(String message) {
        Clients.showNotification(message, Clients.NOTIFICATION_TYPE_WARNING, null, "middle_center", 3000);
    }

    private void error(String message) {
        Clients.showNotification(message, Clients.NOTIFICATION_TYPE_ERROR, null, "middle_center", 4000);
    }
}
