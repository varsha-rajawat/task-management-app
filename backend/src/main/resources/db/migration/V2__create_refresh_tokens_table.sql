-- V2__create_refresh_tokens_table.sql
--
-- Why a DB table for refresh tokens?
-- Access tokens are stateless (verified by signature only).
-- Refresh tokens need to be revocable — e.g. on logout or password change.
-- Storing them in DB lets us delete/invalidate them server-side.

CREATE TABLE refresh_tokens (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMP   NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_token   ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
