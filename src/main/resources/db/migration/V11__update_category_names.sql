-- Update categories to the new 8 categories in order
DELETE FROM categories WHERE slug IN ('property', 'animals-pets', 'agent');

INSERT INTO categories (name, name_en, slug, icon, color, sort_order, moderation_level)
VALUES
    ('ಆಸ್ತಿ ಮಾರಾಟ & ಖರೀದಿ', 'Property Sales and Purchases', 'property-sales', '🏠', 'bg-blue-100', 1, 'CHECKER_ONLY'),
    ('ಆಸ್ತಿ ಬಾಡಿಗೆ & ಲೀಸ್', 'Property Rent and Lease', 'property-rent', '🏢', 'bg-sky-100', 2, 'CHECKER_ONLY'),
    ('ಕೃಷಿ ಉಪಕರಣ', 'Agriculture Equipment', 'agriculture-equipment', '🚜', 'bg-green-100', 3, 'CHECKER_ONLY'),
    ('ಏಜೆಂಟರು', 'Agents', 'agents', '🤝', 'bg-purple-100', 4, 'CHECKER_ONLY'),
    ('ದನಕರುಗಳು', 'Danakarugalu', 'danakarugalu', '🐄', 'bg-amber-100', 5, 'CHECKER_ONLY'),
    ('ಸಾಕು ಪ್ರಾಣಿಗಳು & ಪೆಟ್ಸ್', 'Pets', 'pets', '🐕', 'bg-pink-100', 6, 'CHECKER_ONLY'),
    ('ಕಾರು & ಆಟೋ ಬಾಡಿಗೆ', 'Car and Auto Rent', 'vehicle-rent', '🚗', 'bg-orange-100', 7, 'CHECKER_ONLY'),
    ('ಇತರ ಸೇವೆಗಳು', 'Others Service', 'services', '🔧', 'bg-red-100', 8, 'NO_AUTH')
ON CONFLICT (slug) DO UPDATE SET
    name = EXCLUDED.name,
    name_en = EXCLUDED.name_en,
    icon = EXCLUDED.icon,
    color = EXCLUDED.color,
    sort_order = EXCLUDED.sort_order,
    moderation_level = EXCLUDED.moderation_level;
