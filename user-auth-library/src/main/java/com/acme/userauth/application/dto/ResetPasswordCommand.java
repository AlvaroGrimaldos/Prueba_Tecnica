package com.acme.userauth.application.dto;

import com.acme.userauth.application.validation.StrongPassword;

import jakarta.validation.constraints.NotBlank;

/**
 * Comando de restablecimiento administrativo de contraseña.
 * <p>
 * A diferencia de {@link ChangePasswordCommand}, no exige la contraseña
 * actual: es una operación privilegiada pensada para administradores. La
 * autorización (quién puede invocarla) es responsabilidad del consumidor;
 * la librería solo garantiza la política de la nueva contraseña.
 *
 * @param newPassword nueva contraseña en claro, sujeta a {@link StrongPassword}
 */
public record ResetPasswordCommand(

        @NotBlank
        @StrongPassword
        String newPassword) {

    /** Evita filtrar la contraseña en claro por logs o trazas. */
    @Override
    public String toString() {
        return "ResetPasswordCommand[newPassword=****]";
    }
}
