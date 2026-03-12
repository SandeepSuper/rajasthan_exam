-- ============================================================
-- V2: Community Features + User Block
-- Run Date: 2026-03-12
-- ============================================================

-- 1. Add is_community_blocked to users
--    (community block feature — default false for all existing users)
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS is_community_blocked BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Add exam_id to community_posts
--    (exam-wise community filtering — nullable, null = general post)
ALTER TABLE community_posts
    ADD COLUMN IF NOT EXISTS exam_id UUID REFERENCES exams(id) ON DELETE SET NULL;

-- 3. Add exam_id index for fast filtering
CREATE INDEX IF NOT EXISTS idx_community_posts_exam_id ON community_posts(exam_id);

-- 4. Add is_community_blocked index for admin blocked-users query
CREATE INDEX IF NOT EXISTS idx_users_community_blocked ON users(is_community_blocked)
    WHERE is_community_blocked = TRUE;
