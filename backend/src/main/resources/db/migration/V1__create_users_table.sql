-- V1__create_users_table.sql
--
-- gen_random_uuid() is a PostgreSQL 13+ function that generates a UUID v4.
-- We use it as default so the DB assigns IDs — we never generate them in Java.
-- This avoids collisions in distributed systems and is safer than auto-increment.

CREATE TABLE users (
                       id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                       name       VARCHAR(100) NOT NULL,
                       email      VARCHAR(150) NOT NULL UNIQUE,
                       password   VARCHAR(255) NOT NULL,     -- BCrypt hash, never plain text
                       role       VARCHAR(20)  NOT NULL DEFAULT 'USER',
                       created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Index on email because login lookups always filter by email
CREATE INDEX idx_users_email ON users(email);
