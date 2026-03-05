CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_username ON users(username);

-- Insert default admin user (password is BCrypt hash of "admin")
-- BCrypt hash generated with: new BCryptPasswordEncoder(12).encode("admin")
INSERT INTO users (username, password, role, enabled)
VALUES ('admin', '$2a$12$ljtlkN9lmEQ9n765PZs20O.qLPKsNxE.jkHjTd05SdnC0x4fMTl.i', 'ADMIN', true);
