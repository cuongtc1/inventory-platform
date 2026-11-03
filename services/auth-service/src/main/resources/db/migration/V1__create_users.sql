-- People who can log in. The email is the login name and is stored in lower case.
-- The first ADMIN is not inserted here: auth-service creates it at startup from ADMIN_EMAIL / ADMIN_PASSWORD
-- when this table is empty, so no password hash is committed.
CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(150) NOT NULL,
    -- BCrypt hash ("$2a$10$" + salt + hash): 60 characters; never the password itself
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    -- STAFF, MANAGER or ADMIN (enum UserType)
    user_type     VARCHAR(20)  NOT NULL,
    -- ACTIVE or INACTIVE (enum UserStatus); users are deactivated, never deleted
    status        VARCHAR(20)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    created_by    VARCHAR(100) NOT NULL,
    updated_by    VARCHAR(100) NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);
