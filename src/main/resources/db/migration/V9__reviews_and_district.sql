-- Reviews table for buyer-seller ratings
-- Drop existing reviews table (V4 schema) and recreate with buyer-experience schema
DROP TABLE IF EXISTS reviews;

CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    buyer_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    seller_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating SMALLINT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(buyer_id, seller_id)
);

CREATE INDEX idx_reviews_seller ON reviews(seller_id);
CREATE INDEX idx_reviews_buyer ON reviews(buyer_id);

-- Add preferred_district column to users table for location-based discovery
ALTER TABLE users ADD COLUMN IF NOT EXISTS preferred_district VARCHAR(50);
