-- Add admin fields to users
ALTER TABLE users ADD COLUMN IF NOT EXISTS banned BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS ban_reason TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS banned_at TIMESTAMP;

-- Add moderation fields to listings
ALTER TABLE listings ADD COLUMN IF NOT EXISTS featured BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE listings ADD COLUMN IF NOT EXISTS rejection_reason TEXT;
ALTER TABLE listings ADD COLUMN IF NOT EXISTS moderated_by BIGINT REFERENCES users(id);
ALTER TABLE listings ADD COLUMN IF NOT EXISTS moderated_at TIMESTAMP;

-- Banners table
CREATE TABLE banners (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    subtitle VARCHAR(500),
    image_url VARCHAR(500),
    link VARCHAR(500),
    color VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Platform settings
CREATE TABLE platform_settings (
    key VARCHAR(100) PRIMARY KEY,
    value VARCHAR(500) NOT NULL,
    updated_by BIGINT REFERENCES users(id),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Audit log
CREATE TABLE admin_audit_log (
    id BIGSERIAL PRIMARY KEY,
    actor_id BIGINT NOT NULL REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(50),
    target_id BIGINT,
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_log_actor ON admin_audit_log(actor_id);
CREATE INDEX idx_audit_log_action ON admin_audit_log(action);
CREATE INDEX idx_audit_log_created ON admin_audit_log(created_at DESC);

-- Seed default settings
INSERT INTO platform_settings (key, value) VALUES
    ('contact_unlock_price', '5000'),
    ('max_images_per_listing', '5'),
    ('listing_expiry_days', '30'),
    ('maintenance_mode', 'false')
ON CONFLICT (key) DO NOTHING;

-- Create index for featured listings
CREATE INDEX idx_listings_featured ON listings(featured) WHERE featured = TRUE;
CREATE INDEX idx_listings_moderation ON listings(status) WHERE status = 'PENDING';
