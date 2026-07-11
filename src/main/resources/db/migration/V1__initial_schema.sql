-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(15) NOT NULL UNIQUE,
    email VARCHAR(255) UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    location VARCHAR(255),
    district VARCHAR(255),
    profile_image VARCHAR(500),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

-- Refresh tokens table
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(500) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);

-- OTP table (for phone verification)
CREATE TABLE otp_records (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(15) NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_otp_phone ON otp_records(phone);

-- Listings table
CREATE TABLE listings (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    title_en VARCHAR(500),
    description TEXT,
    category VARCHAR(50) NOT NULL,
    price DOUBLE PRECISION,
    price_unit VARCHAR(50),
    location VARCHAR(255),
    district VARCHAR(255),
    breed VARCHAR(255),
    age VARCHAR(100),
    condition VARCHAR(100),
    hp VARCHAR(50),
    area VARCHAR(100),
    skill VARCHAR(255),
    experience VARCHAR(255),
    vehicle_type VARCHAR(100),
    rate_info VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    view_count INTEGER NOT NULL DEFAULT 0,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE INDEX idx_listings_category ON listings(category, status);
CREATE INDEX idx_listings_user ON listings(user_id);
CREATE INDEX idx_listings_status ON listings(status, created_at DESC);

-- Listing images
CREATE TABLE listing_images (
    listing_id BIGINT NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    image_url VARCHAR(500) NOT NULL
);

CREATE INDEX idx_listing_images ON listing_images(listing_id);

-- Favorites
CREATE TABLE favorites (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    listing_id BIGINT NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, listing_id)
);

CREATE INDEX idx_favorites_user ON favorites(user_id);
