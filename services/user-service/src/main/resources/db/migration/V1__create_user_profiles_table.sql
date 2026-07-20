CREATE TABLE user_profiles (
    user_id VARCHAR(36) PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL,
    bio VARCHAR(500),
    avatar_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
