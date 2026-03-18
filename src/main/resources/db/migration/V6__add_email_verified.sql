-- V6: Add email_verified column to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- Mark existing Google-authenticated users (those with email but no password_hash) as already verified
UPDATE users SET email_verified = TRUE WHERE email IS NOT NULL AND password_hash IS NULL;
