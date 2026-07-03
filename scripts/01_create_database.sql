-- ============================================================================
-- 01_create_database.sql
-- Creación de la base de datos de la aplicación.
--
-- CÓMO EJECUTARLO (DBeaver):
--   1. Conéctate al servidor PostgreSQL con el usuario superusuario "postgres"
--      (la conexión por defecto, apuntando a la base de datos "postgres").
--   2. Abre este script y ejecuta la OPCIÓN A (o la B si prefieres un usuario
--      dedicado). CREATE DATABASE no puede ejecutarse dentro de una
--      transacción: en DBeaver usa "Execute SQL Statement" (Ctrl+Enter)
--      sentencia por sentencia.
--
-- IMPORTANTE: este script solo crea la base de datos vacía. Las TABLAS las
-- crea Flyway automáticamente la primera vez que arranca la aplicación
-- (migración user-auth-library/src/main/resources/db/migration/userauth/
-- V1__create_users.sql). No crees tablas a mano.
-- ============================================================================


-- ────────────────────────────────────────────────────────────────────────────
-- OPCIÓN A — Desarrollo local rápido (la que espera la configuración por
-- defecto de application.yml: usuario "postgres")
-- ────────────────────────────────────────────────────────────────────────────

CREATE DATABASE prueba_tecnica
    WITH ENCODING = 'UTF8'
         TEMPLATE = template0;


-- ────────────────────────────────────────────────────────────────────────────
-- OPCIÓN B — Recomendada para entornos reales: usuario dedicado para la
-- aplicación, sin privilegios de superusuario (principio de mínimo privilegio).
-- Si usas esta opción, descomenta las sentencias y luego configura las
-- variables de entorno DB_USERNAME y DB_PASSWORD antes de arrancar la app.
-- ────────────────────────────────────────────────────────────────────────────

-- ⚠️ CAMBIAR AL USAR: sustituye 'CambiaEstaContrasena1' por una contraseña real.
-- CREATE ROLE prueba_tecnica_app WITH LOGIN PASSWORD 'CambiaEstaContrasena1';

-- El usuario dedicado como OWNER de la base: así tiene permisos completos
-- sobre su esquema sin ser superusuario (necesario en PostgreSQL 15+, donde
-- el esquema public ya no es escribible por cualquiera).
-- CREATE DATABASE prueba_tecnica
--     WITH OWNER    = prueba_tecnica_app
--          ENCODING = 'UTF8'
--          TEMPLATE = template0;
