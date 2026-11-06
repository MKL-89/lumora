-- Security state is isolated in V2 so deployed V1 databases remain migration-safe.
ALTER TABLE users ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN last_failed_login_at DATETIME NULL;
ALTER TABLE users ADD COLUMN lockout_until DATETIME NULL;
ALTER TABLE users ADD COLUMN password_changed_at DATETIME NULL;

CREATE TABLE password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    version BIGINT,
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_password_reset_expiry (expires_at)
);

-- Spring Security JdbcTokenRepositoryImpl persistent remember-me token format.
CREATE TABLE persistent_logins (
    username VARCHAR(64) NOT NULL,
    series VARCHAR(64) PRIMARY KEY,
    token VARCHAR(64) NOT NULL,
    last_used TIMESTAMP NOT NULL
);
