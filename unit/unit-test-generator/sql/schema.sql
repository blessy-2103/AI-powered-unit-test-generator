-- Run this once, or let Hibernate auto-create it (spring.jpa.hibernate.ddl-auto=update)
CREATE DATABASE IF NOT EXISTS testgen_db;
USE testgen_db;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(100) NOT NULL UNIQUE,
    username VARCHAR(100) NOT NULL,
    expires_at DATETIME NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS test_cases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_name VARCHAR(255) NOT NULL,
    source_code LONGTEXT NOT NULL,
    generated_tests LONGTEXT NOT NULL,
    language VARCHAR(50),
    framework VARCHAR(50),
    test_style VARCHAR(30),
    owner_username VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_class_name (class_name),
    INDEX idx_created_at (created_at),
    INDEX idx_owner_username (owner_username)
);
