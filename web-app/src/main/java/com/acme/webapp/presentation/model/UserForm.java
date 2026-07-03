package com.acme.webapp.presentation.model;

import java.util.EnumSet;
import java.util.Set;

import com.acme.userauth.domain.model.Role;

/**
 * Form-backing bean del diálogo de alta de usuario.
 * <p>
 * Mutable a propósito: el binding bidireccional {@code @bind} de ZK necesita
 * getters y setters. Es un objeto exclusivo de la vista; al guardar se
 * convierte en un {@code RegisterUserCommand} inmutable, que es lo único que
 * cruza hacia la capa de aplicación. No contiene lógica de negocio.
 */
public class UserForm {

    private String username = "";
    private String email = "";
    private String password = "";
    private String confirmPassword = "";
    private boolean standardUser = true;
    private boolean admin = false;

    /** Traduce los checkboxes de la vista al conjunto de roles del dominio. */
    public Set<Role> selectedRoles() {
        Set<Role> roles = EnumSet.noneOf(Role.class);
        if (standardUser) {
            roles.add(Role.USER);
        }
        if (admin) {
            roles.add(Role.ADMIN);
        }
        return roles;
    }

    /** Comprobación de UX previa al envío; la política real la valida el backend. */
    public boolean passwordsMatch() {
        return password != null && password.equals(confirmPassword);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public boolean isStandardUser() {
        return standardUser;
    }

    public void setStandardUser(boolean standardUser) {
        this.standardUser = standardUser;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }
}
