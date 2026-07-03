-- ============================================================================
-- 02_consultas_verificacion.sql
-- Consultas de apoyo para verificar (y demostrar en la presentación) que la
-- aplicación creó y usa el esquema correctamente.
--
-- CÓMO EJECUTARLO (DBeaver): conéctate a la base de datos "prueba_tecnica"
-- (no a "postgres") DESPUÉS de haber arrancado la aplicación al menos una vez,
-- que es cuando Flyway crea las tablas y se registra el admin inicial.
-- ============================================================================

-- 1. Historial de migraciones aplicado por Flyway: demuestra que el esquema
--    está versionado y no se creó a mano.
SELECT installed_rank, version, description, success, installed_on
FROM flyway_schema_history
ORDER BY installed_rank;

-- 2. Usuarios registrados. Nota: password_hash es BCrypt — la aplicación
--    nunca guarda contraseñas en claro.
SELECT id, username, email, enabled, created_at, version
FROM users
ORDER BY created_at;

-- 3. Roles por usuario (tabla puente del @ElementCollection).
SELECT u.username, r.role
FROM users u
JOIN user_roles r ON r.user_id = u.id
ORDER BY u.username, r.role;

-- 4. Constraints de unicidad: la red de seguridad última contra registros
--    duplicados en condiciones de carrera (el código traduce su violación a
--    la excepción de dominio UserAlreadyExistsException).
SELECT conname AS constraint_name, contype AS type
FROM pg_constraint
WHERE conrelid = 'users'::regclass
ORDER BY conname;
