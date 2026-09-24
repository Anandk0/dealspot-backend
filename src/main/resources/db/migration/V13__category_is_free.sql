-- Add is_free flag to categories
-- When true, contact details for listings in this category (or its subcategories)
-- are shown for free without requiring payment.

ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS is_free BOOLEAN NOT NULL DEFAULT FALSE;
