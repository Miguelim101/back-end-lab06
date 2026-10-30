--=====================================================================--
--= LAB 7 · HU-SEG-03: CADA TAREA PERTENECE A UN USUARIO (PROPIETARIO) =--
--=====================================================================--
-- Evolución del esquema: se ejecuta después de 002_create_users.sql.
-- Relación: un usuario tiene muchas tareas; una tarea pertenece a un usuario.
--
-- Estrategia para los datos anteriores (NO se borra ninguna tarea):
--   1. Se crea un usuario de prueba "legacy@todo.local" que no puede iniciar sesión
--      (su password_hash no es un hash BCrypt válido, así que ninguna contraseña coincide).
--   2. Se agrega tasks.user_id permitiendo NULL y se asignan a ese usuario todas las tareas previas.
--   3. Solo cuando ya no quedan NULL se aplica NOT NULL, la llave foránea y el índice.
-- Si se desea, un administrador puede reasignar luego esas tareas a un usuario real con un UPDATE.

BEGIN;

-- 1. Usuario de prueba dueño de las tareas creadas antes del Lab 7
INSERT INTO users (name, email, password_hash, role)
VALUES ('Tareas previas (Lab 6)', 'legacy@todo.local', '!sin-inicio-de-sesion', 'USER')
ON CONFLICT (email) DO NOTHING;

-- 2. Nueva columna, primero opcional para poder migrar las filas existentes
ALTER TABLE tasks ADD COLUMN user_id BIGINT;

UPDATE tasks
SET user_id = (SELECT id FROM users WHERE email = 'legacy@todo.local')
WHERE user_id IS NULL;

-- 3. Propietario obligatorio, integridad referencial e índice para las consultas por usuario
ALTER TABLE tasks ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

CREATE INDEX idx_tasks_user_id ON tasks (user_id);

COMMIT;
