package com.acme.userauth.application.port.out;

import java.util.List;
import java.util.Optional;

import com.acme.userauth.application.dto.PageResult;
import com.acme.userauth.domain.model.Role;
import com.acme.userauth.domain.model.User;
import com.acme.userauth.domain.vo.Email;
import com.acme.userauth.domain.vo.UserId;
import com.acme.userauth.domain.vo.Username;

/**
 * Puerto de salida de persistencia de usuarios.
 * <p>
 * La capa de aplicación define lo que <em>necesita</em> del almacén de datos
 * en términos del dominio ({@link User} y sus value objects); la
 * infraestructura lo implementa con JPA/PostgreSQL, pero podría hacerlo con
 * cualquier otra tecnología —incluida una implementación en memoria para
 * tests— sin tocar esta capa (inversión de dependencias).
 */
public interface UserRepositoryPort {

    /** Persiste el agregado (alta o actualización) y devuelve el estado guardado. */
    User save(User user);

    /** Busca por identificador. */
    Optional<User> findById(UserId id);

    /** Busca por nombre de usuario normalizado. */
    Optional<User> findByUsername(Username username);

    /** Indica si ya existe un usuario con ese nombre. */
    boolean existsByUsername(Username username);

    /** Indica si ya existe un usuario con ese correo. */
    boolean existsByEmail(Email email);

    /** Devuelve todos los usuarios. Para listados de UI, preferir {@link #search}. */
    List<User> findAll();

    /**
     * Búsqueda paginada por subcadena de nombre de usuario o correo,
     * ordenada por nombre de usuario. El filtro llega ya normalizado en
     * minúsculas (los valores persistidos también lo están).
     *
     * @param filter subcadena a buscar; vacío = sin filtro
     * @param page   índice de página, base 0
     * @param size   tamaño de página, mínimo 1
     */
    PageResult<User> search(String filter, int page, int size);

    /** Número de usuarios habilitados que poseen el rol dado. */
    long countEnabledUsersWithRole(Role role);

    /** Elimina el usuario indicado; silencioso si no existe. */
    void deleteById(UserId id);
}
