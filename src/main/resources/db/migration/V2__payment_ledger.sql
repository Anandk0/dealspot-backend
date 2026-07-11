-- Payment orders (Razorpay)
CREATE TABLE payment_orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    listing_id BIGINT NOT NULL REFERENCES listings(id),
    razorpay_order_id VARCHAR(255) NOT NULL UNIQUE,
    razorpay_payment_id VARCHAR(255),
    razorpay_signature VARCHAR(500),
    amount INTEGER NOT NULL,  -- in paise
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED',  -- CREATED, PAID, FAILED, REFUNDED
    purpose VARCHAR(50) NOT NULL DEFAULT 'CONTACT_UNLOCK',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    paid_at TIMESTAMP
);

CREATE INDEX idx_payment_orders_user ON payment_orders(user_id);
CREATE INDEX idx_payment_orders_razorpay ON payment_orders(razorpay_order_id);

-- Contact unlock ledger (who unlocked whose contact)
CREATE TABLE contact_unlocks (
    id BIGSERIAL PRIMARY KEY,
    buyer_id BIGINT NOT NULL REFERENCES users(id),
    seller_id BIGINT NOT NULL REFERENCES users(id),
    listing_id BIGINT NOT NULL REFERENCES listings(id),
    payment_order_id BIGINT NOT NULL REFERENCES payment_orders(id),
    unlocked_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(buyer_id, listing_id)
);

CREATE INDEX idx_contact_unlocks_buyer ON contact_unlocks(buyer_id);
CREATE INDEX idx_contact_unlocks_listing ON contact_unlocks(listing_id);
