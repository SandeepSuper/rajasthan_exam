-- V10: Fix test_attempts table — add missing columns and fix score column type

-- Add coins_earned column (added to entity but never migrated)
ALTER TABLE test_attempts ADD COLUMN IF NOT EXISTS coins_earned INTEGER DEFAULT 0;

-- Add correct_answers and wrong_answers if somehow V8 missed them
ALTER TABLE test_attempts ADD COLUMN IF NOT EXISTS correct_answers INTEGER;
ALTER TABLE test_attempts ADD COLUMN IF NOT EXISTS wrong_answers INTEGER;

-- Fix score column type: entity uses Double but V1 created it as INTEGER
ALTER TABLE test_attempts ALTER COLUMN score TYPE DOUBLE PRECISION USING score::DOUBLE PRECISION;
