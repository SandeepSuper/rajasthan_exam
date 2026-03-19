-- V7: Add missing user columns that were never captured in a migration
-- These columns exist in the User entity but were manually added to dev DB only,
-- causing Hibernate schema validation to fail on a fresh prod DB.

ALTER TABLE users ADD COLUMN IF NOT EXISTS coins INTEGER DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_picture TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS refer_code VARCHAR(20) UNIQUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS referred_count INTEGER DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS referred_by VARCHAR(20);

-- Create index on refer_code for fast referral lookups
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_refer_code ON users(refer_code) WHERE refer_code IS NOT NULL;
