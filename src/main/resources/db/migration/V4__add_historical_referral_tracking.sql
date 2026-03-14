-- Add columns to track exact amounts of referral rewards snapshot
ALTER TABLE users ADD COLUMN referrer_reward_amount INTEGER;
ALTER TABLE users ADD COLUMN historical_referral_coins_earned INTEGER DEFAULT 0;

-- Backfill legacy data (Assume existing referrals earned the current default 50 coins)
UPDATE users SET historical_referral_coins_earned = COALESCE(referred_count, 0) * 50 WHERE referred_count > 0;
UPDATE users SET referrer_reward_amount = 50 WHERE referred_by IS NOT NULL;
