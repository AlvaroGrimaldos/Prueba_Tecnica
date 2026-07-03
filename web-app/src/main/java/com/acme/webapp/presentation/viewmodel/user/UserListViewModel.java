package com.acme.webapp.presentation.viewmodel.user;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.context.SecurityContextHolder;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.GlobalCommand;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.Clients;
import com.acme.webapp.presentation.zk.SpringDelegatingVariableResolver;
import org.zkoss.zul.Messagebox;

import com.acme.userauth.application.port.in.UserService;
import com.acme.userauth.domain.exception.UserOperationNotAllowedException;
import com.acme.webapp.infrastructure.security.ZkLoginService;
import com.acme.webapp.presentation.model.UserRow;

/**
 * ViewModel del listado de usuarios ({@code user-list.zul}).
 * <p>
 * Patrón MVVM de ZK: la vista se enlaza a este modelo mediante
 * {@code BindComposer}; aquí no se toca ningún componente ZK de la pantalla.
 * Toda la lógica de negocio vive detrás de los puertos de
 * {@code user-auth-library}; este ViewModel orquesta la interacción, pagina
 * y adapta datos para la vista.
 * <p>
 * Guardas de presentación: las acciones sobre la propia cuenta se cortan
 * aquí con un aviso amable (UX); la regla de negocio de verdad —no dejar el
 * sistema sin administradores— la garantiza el dominio y aquí solo se
 * traduce a notificación si llega a dispararse.
 */
@VariableResolver(SpringDelegatingVariableResolver.class)
public class UserListViewModel {

    /** Comando global que refresca el listado desde cualquier diálogo. */
    public static final String GC_REFRESH_USERS = "refreshUsers";

    // Prefijo ~./ = recurso del classpath (src/main/resources/web/), la forma
    // correcta de referenciar zul internos en empaquetado jar
    private static final String USER_FORM_ZUL = "~./zul/user/user-form.zul";
    private static final String RESET_PASSWORD_ZUL = "~./zul/user/reset-password.zul";
    private static final String CHANGE_PASSWORD_ZUL = "~./zul/user/change-password.zul";
    private static final int PAGE_SIZE = 10;

    @WireVariable("userService")
    private UserService userService;

    @WireVariable("zkLoginService")
    private ZkLoginService zkLoginService;

    private String filter = "";
    private List<UserRow> users = List.of();
    private int total;
    private int activePage;

    @Init
    public void init() {
        loadUsers();
    }

    /* ---------------------------------------------------------------- *
     *  Comandos de listado                                             *
     * ---------------------------------------------------------------- */

    /** Aplica el filtro de búsqueda y vuelve a la primera página. */
    @Command
    @NotifyChange({"users", "total", "activePage"})
    public void search() {
        activePage = 0;
        loadUsers();
    }

    /** Cambio de página desde el componente paging. */
    @Command
    @NotifyChange({"users", "total"})
    public void paginate() {
        loadUsers();
    }

    /** Punto de entrada de los diálogos para refrescar tras guardar. */
    @GlobalCommand(GC_REFRESH_USERS)
    @NotifyChange({"users", "total", "activePage"})
    public void refreshUsers() {
        loadUsers();
    }

    /* ---------------------------------------------------------------- *
     *  Comandos de acciones                                            *
     * ---------------------------------------------------------------- */

    /** Abre el diálogo modal de alta de usuario. */
    @Command
    public void openCreateForm() {
        Executions.createComponents(USER_FORM_ZUL, null, null);
    }

    /** Abre el restablecimiento administrativo para el usuario indicado. */
    @Command
    public void openResetPassword(@BindingParam("user") UserRow user) {
        Executions.createComponents(RESET_PASSWORD_ZUL, null,
                Map.of("userId", user.getId(), "username", user.getUsername()));
    }

    /** Abre el cambio de contraseña de la cuenta propia. */
    @Command
    public void openMyPassword() {
        Executions.createComponents(CHANGE_PASSWORD_ZUL, null, null);
    }

    /** Cierra la sesión y vuelve al login. */
    @Command
    public void logout() {
        zkLoginService.logout();
        Executions.sendRedirect("/");
    }

    /** Alterna el estado habilitado/deshabilitado de la cuenta. */
    @Command
    @NotifyChange({"users", "total"})
    public void toggleEnabled(@BindingParam("user") UserRow user) {
        if (isOwnAccount(user)) {
            warn("No puedes desactivar tu propia sesión activa");
            return;
        }
        try {
            if (user.isEnabled()) {
                userService.disable(user.getId());
            } else {
                userService.enable(user.getId());
            }
        } catch (UserOperationNotAllowedException ex) {
            error("Operación no permitida: es el último administrador activo");
        }
        loadUsers();
    }

    /**
     * Pide confirmación y elimina el usuario. El borrado ocurre en el
     * callback asíncrono del {@link Messagebox}, fuera de la fase de binding,
     * por lo que el refresco se publica como comando global con
     * {@link BindUtils} en lugar de {@code @NotifyChange}.
     */
    @Command
    public void confirmDelete(@BindingParam("user") UserRow user) {
        if (isOwnAccount(user)) {
            warn("No puedes eliminar tu propia cuenta");
            return;
        }
        Messagebox.show(
                "¿Seguro que quieres eliminar al usuario \"%s\"? Esta acción no se puede deshacer."
                        .formatted(user.getUsername()),
                "Eliminar usuario",
                Messagebox.OK | Messagebox.CANCEL,
                Messagebox.EXCLAMATION,
                event -> {
                    if (!Messagebox.ON_OK.equals(event.getName())) {
                        return;
                    }
                    try {
                        userService.delete(user.getId());
                        Clients.showNotification("Usuario eliminado",
                                Clients.NOTIFICATION_TYPE_INFO, null, "top_center", 2500);
                    } catch (UserOperationNotAllowedException ex) {
                        Clients.showNotification(
                                "Operación no permitida: es el último administrador activo",
                                Clients.NOTIFICATION_TYPE_ERROR, null, "middle_center", 4000);
                    }
                    BindUtils.postGlobalCommand(null, null, GC_REFRESH_USERS, null);
                });
    }

    /* ---------------------------------------------------------------- *
     *  Estado para la vista                                            *
     * ---------------------------------------------------------------- */

    public List<UserRow> getUsers() {
        return users;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter == null ? "" : filter;
    }

    public int getTotal() {
        return total;
    }

    public int getActivePage() {
        return activePage;
    }

    public void setActivePage(int activePage) {
        this.activePage = Math.max(0, activePage);
    }

    public int getPageSize() {
        return PAGE_SIZE;
    }

    /** Nombre del usuario autenticado, para la cabecera. */
    public String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private boolean isOwnAccount(UserRow user) {
        return user.getUsername().equals(getCurrentUsername());
    }

    private void loadUsers() {
        var result = userService.search(filter, activePage, PAGE_SIZE);
        // Si la página activa quedó fuera de rango (p. ej. tras eliminar el
        // último elemento de la última página), retrocede a la última válida
        if (result.items().isEmpty() && result.total() > 0 && activePage > 0) {
            activePage = result.totalPages() - 1;
            result = userService.search(filter, activePage, PAGE_SIZE);
        }
        users = result.items().stream().map(UserRow::from).toList();
        total = (int) Math.min(result.total(), Integer.MAX_VALUE);
    }

    private void warn(String message) {
        Clients.showNotification(message, Clients.NOTIFICATION_TYPE_WARNING, null, "middle_center", 3000);
    }

    private void error(String message) {
        Clients.showNotification(message, Clients.NOTIFICATION_TYPE_ERROR, null, "middle_center", 4000);
    }
}
