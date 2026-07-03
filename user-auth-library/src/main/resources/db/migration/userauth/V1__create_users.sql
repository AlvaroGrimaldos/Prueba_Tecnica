-- Esquema de user-auth-library.
-- Convención de versiones: la librería usa V1–V99; las aplicaciones
-- consumidoras deben empezar en V100 para evitar colisiones de versión
-- entre locations de Flyway.

CREATE TABLE users (
    id            UUID         NOT NULL,
    username      VARCHAR(50)  NOT NULL,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    enabled       BOOLEAN      NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    version       BIGINT       NOT NULL,

    CONSTRAINT pk_users PRIMARY KEY (id),
    -- La regla de negocio de unicidad vive en el dominio; estas constraints
    -- son la garantía última frente a condiciones de carrera. Sus nombres
    -- son contrato: el adaptador de persistencia los usa para traducir la
    -- violación a la excepción de dominio correspondiente.
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE user_roles (
    user_id UUID        NOT NULL,
    role    VARCHAR(30) NOT NULL,

    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE
);
