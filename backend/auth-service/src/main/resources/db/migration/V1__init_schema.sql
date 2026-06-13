-- V1__init_schema.sql
-- Initial schema for foodzie auth-service

CREATE TABLE IF NOT EXISTS permissions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  DATETIME     NOT NULL
);

CREATE TABLE IF NOT EXISTS roles (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  DATETIME     NOT NULL
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role       FOREIGN KEY (role_id)       REFERENCES roles(id)       ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS users (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    email                 VARCHAR(255) NOT NULL UNIQUE,
    username              VARCHAR(100) NOT NULL UNIQUE,
    password_hash         VARCHAR(255),
    first_name            VARCHAR(100),
    last_name             VARCHAR(100),
    phone_number          VARCHAR(30),
    profile_picture_url   VARCHAR(500),
    status                VARCHAR(30)  NOT NULL DEFAULT 'PENDING_VERIFICATION',
    email_verified        TINYINT(1)   NOT NULL DEFAULT 0,
    mfa_enabled           TINYINT(1)   NOT NULL DEFAULT 0,
    mfa_secret            VARCHAR(255),
    password_reset_token  VARCHAR(255),
    password_reset_expiry DATETIME,
    last_login_at         DATETIME,
    created_at            DATETIME     NOT NULL,
    updated_at            DATETIME     NOT NULL,
    deleted_at            DATETIME
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    device_info VARCHAR(500),
    ip_address  VARCHAR(45),
    expires_at  DATETIME     NOT NULL,
    revoked     TINYINT(1)   NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL,
    CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS mfa_tokens (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    token      VARCHAR(10) NOT NULL,
    type       VARCHAR(20) NOT NULL,
    used       TINYINT(1)  NOT NULL DEFAULT 0,
    expires_at DATETIME    NOT NULL,
    created_at DATETIME    NOT NULL,
    CONSTRAINT fk_mt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS oauth_connections (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT       NOT NULL,
    provider         VARCHAR(50)  NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    access_token     TEXT,
    refresh_token    TEXT,
    token_expiry     DATETIME,
    connected_at     DATETIME     NOT NULL,
    UNIQUE KEY uq_oauth_provider (provider, provider_user_id),
    CONSTRAINT fk_oc_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT,
    actor_email VARCHAR(255),
    action      VARCHAR(100) NOT NULL,
    resource    VARCHAR(100),
    resource_id VARCHAR(255),
    ip_address  VARCHAR(45),
    user_agent  VARCHAR(500),
    status      VARCHAR(10)  NOT NULL DEFAULT 'SUCCESS',
    details     JSON,
    created_at  DATETIME     NOT NULL
);
