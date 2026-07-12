-- Centralized media/file storage tracking
CREATE TABLE media (
    id BIGSERIAL PRIMARY KEY,
    cloudinary_public_id VARCHAR(500) NOT NULL,
    cloudinary_url VARCHAR(1000) NOT NULL,
    secure_url VARCHAR(1000) NOT NULL,
    file_name VARCHAR(255),
    file_type VARCHAR(50),
    file_size BIGINT,
    width INTEGER,
    height INTEGER,
    format VARCHAR(20),
    resource_type VARCHAR(20) NOT NULL DEFAULT 'image',
    folder VARCHAR(255),
    entity_type VARCHAR(50),
    entity_id BIGINT,
    uploaded_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_media_entity ON media(entity_type, entity_id);
CREATE INDEX idx_media_uploader ON media(uploaded_by);
CREATE INDEX idx_media_public_id ON media(cloudinary_public_id);
