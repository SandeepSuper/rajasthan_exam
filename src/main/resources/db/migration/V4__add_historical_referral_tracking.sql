-- Add columns to track exact amounts of referral rewards snapshot
ALTER TABLE users ADD COLUMN IF NOT EXISTS referrer_reward_amount INTEGER;
ALTER TABLE users ADD COLUMN IF NOT EXISTS historical_referral_coins_earned INTEGER DEFAULT 0;

-- Backfill legacy data only if referred_count / referred_by columns already exist
-- (they exist on dev/migrated DBs but not on fresh prod DBs)
DO $$
BEGIN
    -- Backfill historical coins if referred_count column exists
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'referred_count'
    ) THEN
        EXECUTE 'UPDATE users SET historical_referral_coins_earned = COALESCE(referred_count, 0) * 50 WHERE referred_count > 0';
    END IF;

    -- Backfill referrer_reward_amount if referred_by column exists
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'referred_by'
    ) THEN
        EXECUTE 'UPDATE users SET referrer_reward_amount = 50 WHERE referred_by IS NOT NULL';
    END IF;
END $$;
