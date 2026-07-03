package com.acme.userauth.application.port.in;

import java.util.List;
import java.util.UUID;

import com.acme.userauth.application.dto.ChangePasswordCommand;
import com.acme.userauth.application.dto.PageResult;
import com.acme.userauth.application.dto.RegisterUserCommand;
import com.acme.userauth.application.dto.ResetPasswordCommand;
import com.acme.userauth.application.dto.UserResponse;

import jakarta.validation.Valid;

/**
 * Puerto de entrada para la gestión del ciclo de vida de usuarios.
 * <p>
 * Es el contrato público que la librería ofrece a sus consumidores (la
 * web-app ZK, un API REST, tests…): trabajan contra esta abstracción y contra
 * los DTOs, nunca contra el modelo de dominio ni la implementación (DIP).
 * <p>
 * Las anotaciones {@code @Valid} activan la validación declarativa de los
 * comandos cuando la implementación se ejecuta bajo un contenedor con method
 * validation habilitada (Spring lo hace vía {@code @Validated}).
 */
public interface UserService {

    /**
     * Registra un usuario nuevo.
     *
     * @param command datos de alta, validados sintácticamente
     * @return representación del usuario creado
     * @throws com.acme.userauth.domain.exception.UserAlreadyExistsException
     *         si el nombre de usuario o el correo ya están en uso
     */
    UserResponse register(@Valid RegisterUserCommand command);

    /**
     * Busca un usuario por identificador.
     *
     * @throws com.acme.userauth.domain.exception.UserNotFoundException si no existe
     */
    UserResponse findById(UUID id);

    /**
     * Busca un usuario por nombre de usuario (insensible a mayúsculas).
     *
     * @throws com.acme.userauth.domain.exception.UserNotFoundException si no existe
     */
    UserResponse findByUsername(String username);

    /** Lista todos los usuarios registrados. Para vistas, preferir {@link #search}. */
    List<UserResponse> findAll();

    /**
     * Búsqueda paginada por nombre de usuario o correo (subcadena,
     * insensible a mayúsculas). Con filtro vacío devuelve todos, paginados
     * y ordenados por nombre de usuario.
     *
     * @param filter texto a buscar; {@code null} o vacío = sin filtro
     * @param page   índice de página, base 0
     * @param size   tamaño de página (mínimo 1)
     */
    PageResult<UserResponse> search(String filter, int page, int size);

    /**
     * Cambia la contraseña de un usuario tras verificar la actual.
     *
     * @throws com.acme.userauth.domain.exception.UserNotFoundException
     *         si el usuario no existe
     * @throws com.acme.userauth.domain.exception.AuthenticationException
     *         si la contraseña actual no coincide
     */
    void changePassword(UUID userId, @Valid ChangePasswordCommand command);

    /**
     * Restablece la contraseña sin verificar la actual (operación
     * administrativa). La autorización de quién puede invocarla es
     * responsabilidad del consumidor.
     *
     * @throws com.acme.userauth.domain.exception.UserNotFoundException
     *         si el usuario no existe
     */
    void resetPassword(UUID userId, @Valid ResetPasswordCommand command);

    /**
     * Deshabilita la cuenta (borrado lógico a efectos de autenticación).
     *
     * @throws com.acme.userauth.domain.exception.UserNotFoundException si no existe
     * @throws com.acme.userauth.domain.exception.UserOperationNotAllowedException
     *         si es el último administrador habilitado
     */
    void disable(UUID userId);

    /**
     * Vuelve a habilitar una cuenta deshabilitada.
     *
     * @throws com.acme.userauth.domain.exception.UserNotFoundException si no existe
     */
    void enable(UUID userId);

    /**
     * Elimina definitivamente al usuario.
     *
     * @throws com.acme.userauth.domain.exception.UserNotFoundException si no existe
     * @throws com.acme.userauth.domain.exception.UserOperationNotAllowedException
     *         si es el último administrador habilitado
     */
    void delete(UUID userId);
}
