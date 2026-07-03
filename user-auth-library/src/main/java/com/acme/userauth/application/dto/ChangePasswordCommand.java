package com.acme.userauth.application.dto;

import com.acme.userauth.application.validation.StrongPassword;

import jakarta.validation.constraints.NotBlank;

/**
 * Comando de cambio de contraseña de un usuario existente.
 *
 * @param currentPassword contraseña actual en claro, para verificar identidad
 * @param newPassword     nueva contraseña en claro, sujeta a la política
 *                        {@link StrongPassword}
 */
public record ChangePasswordCommand(

        @NotBlank
        String currentPassword,

        @NotBlank
        @StrongPassword
        String newPassword) {

    /** Evita filtrar contraseñas en claro por logs o trazas. */
    @Override
    public String toString() {
        return "ChangePasswordCommand[currentPassword=****, newPassword=****]";
    }
}
