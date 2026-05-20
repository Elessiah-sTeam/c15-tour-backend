ALTER TABLE users ADD COLUMN password_changed_at BIGINT;
UPDATE users SET reset_token = NULL, reset_token_expiry = NULL;
