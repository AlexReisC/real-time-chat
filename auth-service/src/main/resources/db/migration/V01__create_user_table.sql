CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(144) NOT NULL,
    account_non_expired BOOLEAN NOT NULL DEFAULT false,
    account_non_locked BOOLEAN NOT NULL DEFAULT false,
    credentials_non_expired BOOLEAN NOT NULL DEFAULT false,
    enabled BOOLEAN NOT NULL DEFAULT true
)