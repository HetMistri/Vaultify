-- 1. Create the database
CREATE DATABASE vaultify;

-- 2. Connect to the database (e.g., \c vaultify) and run the schema:

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    public_key TEXT NOT NULL,
    private_key_encrypted TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE credentials (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    filename TEXT,
    filepath TEXT UNIQUE NOT NULL,
    metadata TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tokens (
    id SERIAL PRIMARY KEY,
    credential_id INT REFERENCES credentials(id) ON DELETE CASCADE,
    token TEXT UNIQUE NOT NULL,
    expiry TIMESTAMP NOT NULL
);