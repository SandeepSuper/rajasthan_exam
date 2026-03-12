-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Users Table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    mobile VARCHAR(15) NOT NULL UNIQUE,
    email VARCHAR(255),
    password_hash VARCHAR(255),
    name VARCHAR(255),
    role VARCHAR(20) DEFAULT 'STUDENT', -- ADMIN, STUDENT
    is_premium BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Exams Table
CREATE TABLE exams (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(255) NOT NULL,
    icon_url TEXT,
    category VARCHAR(50) NOT NULL, -- CET, REET, etc.
    language_supported VARCHAR(10) DEFAULT 'BOTH' -- HI, EN, BOTH
);

-- 3. Tests Table (Renamed from test_series)
CREATE TABLE tests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    exam_id UUID NOT NULL REFERENCES exams(id),
    title VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL, -- MOCK, TOPIC, PYQ
    duration_minutes INTEGER DEFAULT 60,
    total_questions INTEGER DEFAULT 0,
    is_premium BOOLEAN DEFAULT FALSE
);

-- 4. Questions Table
CREATE TABLE questions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    test_id UUID NOT NULL REFERENCES tests(id),
    text_hi TEXT,
    text_en TEXT,
    options_hi JSONB, -- Array of strings
    options_en JSONB, -- Array of strings
    correct_option_index INTEGER NOT NULL,
    solution_hi TEXT,
    solution_en TEXT
);

-- 5. News Items (Current Affairs)
CREATE TABLE news_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title_hi TEXT NOT NULL,
    title_en TEXT,
    desc_hi TEXT,
    desc_en TEXT,
    date DATE DEFAULT CURRENT_DATE,
    image_url TEXT
);

-- 6. Community Posts
CREATE TABLE community_posts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    content TEXT NOT NULL,
    image_url TEXT,
    likes_count INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 7. Test Attempts
CREATE TABLE test_attempts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    test_id UUID NOT NULL REFERENCES tests(id),
    score INTEGER NOT NULL,
    total_questions INTEGER NOT NULL,
    time_taken INTEGER, -- seconds
    accuracy FLOAT,
    attempt_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 8. Bookmarks
CREATE TABLE bookmarks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    entity_type VARCHAR(20) NOT NULL, -- QUESTION, NEWS
    entity_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_bookmark UNIQUE (user_id, entity_type, entity_id)
);

-- 9. Referrals
CREATE TABLE referrals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    referrer_id UUID NOT NULL REFERENCES users(id),
    referee_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(20) DEFAULT 'PENDING',
    coins_earned INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for Performance
CREATE INDEX idx_users_mobile ON users(mobile);
CREATE INDEX idx_tests_exam_id ON tests(exam_id);
CREATE INDEX idx_questions_test_id ON questions(test_id);
CREATE INDEX idx_test_attempts_user_id ON test_attempts(user_id);
