package com.acme.userauth.application.dto;

import java.util.Set;

import com.acme.userauth.application.validation.StrongPassword;
import com.acme.userauth.domain.model.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Comando de alta de usuario: datos de entrada del caso de uso de registro.
 * <p>
 * Las anotaciones de Bean Validation cubren la validación sintáctica en la
 * frontera de la aplicación; los value objects del dominio ({@code Username},
 * {@code Email}) vuelven a garantizar las invariantes en construcción, de
 * modo que el dominio es válido incluso si un llamante omite la validación.
 *
 * @param username    nombre de usuario deseado
 * @param email       correo electrónico del usuario
 * @param rawPassword contraseña en claro; se codifica en el servicio y nunca
 *                    se persiste ni se registra en logs
 * @param roles       roles iniciales; al menos uno
 */
public record RegisterUserCommand(

        @NotBlank
        @Size(min = 3, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$",
                 message = "username may contain only letters, digits, '.', '_' or '-'")
        String username,

        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @NotBlank
        @StrongPassword
        String rawPassword,

        @NotEmpty
        Set<Role> roles) {

    /** Copia defensiva para mantener el comando inmutable. */
    public RegisterUserCommand {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    /** Evita filtrar la contraseña en claro por logs o trazas. */
    @Override
    public String toString() {
        return "RegisterUserCommand[username=%s, email=%s, rawPassword=****, roles=%s]"
                .formatted(username, email, roles);
    }
}
