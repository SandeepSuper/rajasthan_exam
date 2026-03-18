-- V8: Add all missing tables and columns that exist in entities but not in any migration
-- These were manually created on dev DB and never captured as Flyway migrations

-- ═══════════════════════════════════════════════════════════
-- MISSING TABLES
-- ═══════════════════════════════════════════════════════════

-- community_comments
CREATE TABLE IF NOT EXISTS community_comments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    post_id UUID NOT NULL,
    user_id UUID NOT NULL,
    user_name VARCHAR(255) NOT NULL,
    user_profile_picture TEXT,
    content VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- community_likes
CREATE TABLE IF NOT EXISTS community_likes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    post_id UUID NOT NULL,
    user_id UUID NOT NULL,
    CONSTRAINT uq_community_likes_post_user UNIQUE (post_id, user_id)
);

-- notifications
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(255) NOT NULL,
    message VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    icon_type VARCHAR(50) NOT NULL DEFAULT 'info'
);

-- orders
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id VARCHAR(255) NOT NULL,
    exam_id VARCHAR(255) NOT NULL,
    razorpay_order_id VARCHAR(255) NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    payment_id VARCHAR(255),
    coins_used INTEGER NOT NULL DEFAULT 0
);

-- purchases
CREATE TABLE IF NOT EXISTS purchases (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id VARCHAR(255) NOT NULL,
    exam_id VARCHAR(255) NOT NULL,
    purchased_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- question_reports
CREATE TABLE IF NOT EXISTS question_reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id VARCHAR(255) NOT NULL,
    test_id VARCHAR(255) NOT NULL,
    question_id VARCHAR(255) NOT NULL,
    bug_type VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ═══════════════════════════════════════════════════════════
-- MISSING COLUMNS ON EXISTING TABLES
-- ═══════════════════════════════════════════════════════════

-- exams: missing columns added after V1
ALTER TABLE exams ADD COLUMN IF NOT EXISTS negative_marks DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE exams ADD COLUMN IF NOT EXISTS marks_per_question DOUBLE PRECISION DEFAULT 1.0;
ALTER TABLE exams ADD COLUMN IF NOT EXISTS is_premium BOOLEAN DEFAULT FALSE;
ALTER TABLE exams ADD COLUMN IF NOT EXISTS price DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE exams ADD COLUMN IF NOT EXISTS currency VARCHAR(10) DEFAULT 'INR';
ALTER TABLE exams ADD COLUMN IF NOT EXISTS discount_percent INTEGER DEFAULT 0;

-- tests: missing columns added after V1
ALTER TABLE tests ADD COLUMN IF NOT EXISTS is_live BOOLEAN DEFAULT FALSE;
ALTER TABLE tests ADD COLUMN IF NOT EXISTS starts_at TIMESTAMP;
ALTER TABLE tests ADD COLUMN IF NOT EXISTS ends_at TIMESTAMP;
ALTER TABLE tests ADD COLUMN IF NOT EXISTS allow_previous BOOLEAN DEFAULT TRUE;
ALTER TABLE tests ADD COLUMN IF NOT EXISTS allow_solution BOOLEAN DEFAULT TRUE;
ALTER TABLE tests ADD COLUMN IF NOT EXISTS section_lock BOOLEAN DEFAULT FALSE;
ALTER TABLE tests ADD COLUMN IF NOT EXISTS show_result_immediately BOOLEAN DEFAULT TRUE;
ALTER TABLE tests ADD COLUMN IF NOT EXISTS marks_per_question_override DOUBLE PRECISION;
ALTER TABLE tests ADD COLUMN IF NOT EXISTS total_marks DOUBLE PRECISION;

-- community_posts: missing columns added after V1 (V2 added exam_id, these are additional)
ALTER TABLE community_posts ADD COLUMN IF NOT EXISTS user_name VARCHAR(255);
ALTER TABLE community_posts ADD COLUMN IF NOT EXISTS user_profile_picture TEXT;
ALTER TABLE community_posts ADD COLUMN IF NOT EXISTS subject VARCHAR(255);
ALTER TABLE community_posts ADD COLUMN IF NOT EXISTS category VARCHAR(255);
ALTER TABLE community_posts ADD COLUMN IF NOT EXISTS upvotes INTEGER NOT NULL DEFAULT 0;
ALTER TABLE community_posts ADD COLUMN IF NOT EXISTS comment_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE community_posts ADD COLUMN IF NOT EXISTS view_count INTEGER DEFAULT 0;
ALTER TABLE community_posts ADD COLUMN IF NOT EXISTS verified_answer VARCHAR(1000);

-- test_attempts: missing columns
ALTER TABLE test_attempts ADD COLUMN IF NOT EXISTS correct_answers INTEGER;
ALTER TABLE test_attempts ADD COLUMN IF NOT EXISTS wrong_answers INTEGER;

-- ═══════════════════════════════════════════════════════════
-- INDEXES
-- ═══════════════════════════════════════════════════════════
CREATE INDEX IF NOT EXISTS idx_community_comments_post_id ON community_comments(post_id);
CREATE INDEX IF NOT EXISTS idx_community_likes_post_id ON community_likes(post_id);
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_purchases_user_id ON purchases(user_id);
CREATE INDEX IF NOT EXISTS idx_purchases_exam_id ON purchases(exam_id);
