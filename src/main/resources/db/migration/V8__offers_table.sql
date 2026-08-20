-- Offers table for buyer-seller negotiations
CREATE TABLE offers (
    id BIGSERIAL PRIMARY KEY,
    listing_id BIGINT NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    buyer_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    seller_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount DECIMAL(12,2) NOT NULL,
    message TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    counter_amount DECIMAL(12,2),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

-- Partial unique index: only one PENDING offer per buyer per listing
CREATE UNIQUE INDEX idx_offers_one_pending_per_buyer_listing
    ON offers(listing_id, buyer_id) WHERE status = 'PENDING';

-- Indexes for common queries
CREATE INDEX idx_offers_buyer ON offers(buyer_id);
CREATE INDEX idx_offers_seller_status ON offers(seller_id, status);
CREATE INDEX idx_offers_listing ON offers(listing_id);
