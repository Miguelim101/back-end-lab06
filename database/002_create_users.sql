--=====================================================================--
--= LAB 7 · HU-SEG-01: TABLA DE USUARIOS PARA EL REGISTRO Y EL LOGIN  =--
--=====================================================================--
-- Evolución del esquema: se ejecuta después de 001_create_schema.sql.
-- No modifica la tabla tasks (la relación de propiedad llega en 003).

BEGIN;

CREATE TABLE users (

    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,  -- Hash BCrypt (60 caracteres), nunca la contraseña en texto plano
    role VARCHAR(10) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- El correo identifica de manera única a cada usuario
    CONSTRAINT uq_users_email
        UNIQUE (email),

    -- El back-end normaliza el correo a minúsculas; la BD lo garantiza para evitar duplicados equivalentes
    CONSTRAINT chk_users_email_lowercase
        CHECK (email = LOWER(email)),

    CONSTRAINT chk_users_role
        CHECK (role IN ('USER', 'ADMIN'))
);

COMMIT;
