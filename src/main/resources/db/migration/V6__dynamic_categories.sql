-- Dynamic categories table to replace hardcoded category list
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    name_en VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    icon VARCHAR(50),
    image_url VARCHAR(500),
    color VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    moderation_level VARCHAR(30) NOT NULL DEFAULT 'CHECKER_ONLY',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE INDEX idx_categories_slug ON categories(slug);
CREATE INDEX idx_categories_active ON categories(active);
CREATE INDEX idx_categories_sort ON categories(sort_order);

-- Seed with existing hardcoded categories
INSERT INTO categories (name, name_en, slug, icon, color, sort_order, moderation_level) VALUES
    ('ಆಸ್ತಿ ಮಾರಾಟ & ಬಾಡಿಗೆ', 'Property Sale & Rent', 'property', '🏠', 'bg-blue-100', 1, 'CHECKER_ONLY'),
    ('ಕೃಷಿ ಉಪಕರಣ', 'Agriculture Equipment', 'agriculture-equipment', '🚜', 'bg-green-100', 2, 'CHECKER_ONLY'),
    ('ಕಾರು & ಆಟೋ ಬಾಡಿಗೆ', 'Car & Auto Rent', 'vehicle-rent', '🚗', 'bg-orange-100', 3, 'CHECKER_ONLY'),
    ('ಪ್ರಾಣಿಗಳು & ಪೆಟ್ಸ್', 'Animals & Pets', 'animals-pets', '🐄', 'bg-amber-100', 4, 'CHECKER_ONLY'),
    ('ಏಜೆಂಟ್', 'Agent', 'agent', '🤝', 'bg-purple-100', 5, 'CHECKER_ONLY'),
    ('ಇತರ ಸೇವೆಗಳು', 'Other Services', 'services', '🔧', 'bg-red-100', 6, 'NO_AUTH');
