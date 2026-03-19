CREATE TABLE app_config (
    id BIGINT NOT NULL PRIMARY KEY,
    play_store_url VARCHAR(255) NOT NULL,
    referrer_coin_reward INTEGER NOT NULL,
    referee_coin_reward INTEGER NOT NULL,
    share_message_template VARCHAR(1000) NOT NULL
);

INSERT INTO app_config (id, play_store_url, referrer_coin_reward, referee_coin_reward, share_message_template)
VALUES (
    1,
    'https://play.google.com/store/apps/details?id=com.rajasthanexams',
    50,
    20,
    'Join Rajasthan Exam Prep and ace your government exams! 🎓
Use my referral code: {CODE} when signing up to get FREE coins!
Download now: {URL}'
);
