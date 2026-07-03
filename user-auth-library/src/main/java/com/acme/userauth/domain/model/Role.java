package com.acme.userauth.domain.model;

/**
 * Roles de autorización que puede tener un usuario.
 * <p>
 * Se modela como enum por simplicidad y seguridad de tipos; si el proyecto
 * evolucionara hacia roles dinámicos administrables en base de datos, este
 * tipo se convertiría en una entidad propia sin afectar al resto del dominio.
 */
public enum Role {

    /** Acceso completo de administración. */
    ADMIN,

    /** Usuario estándar de la aplicación. */
    USER
}
