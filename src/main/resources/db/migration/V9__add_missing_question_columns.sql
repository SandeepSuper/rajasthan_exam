-- V9: Add missing columns to questions table
-- Moved from V8 (which had already run on prod) to avoid checksum mismatch

ALTER TABLE questions ADD COLUMN IF NOT EXISTS subject VARCHAR(255);
ALTER TABLE questions ADD COLUMN IF NOT EXISTS marks_per_question DOUBLE PRECISION;
ALTER TABLE questions ADD COLUMN IF NOT EXISTS negative_marks DOUBLE PRECISION;
