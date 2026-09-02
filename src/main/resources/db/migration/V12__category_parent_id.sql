-- Step 1: Add parent_id to categories for subcategory support
-- Self-referencing FK: null = top-level category, set = subcategory

ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS parent_id BIGINT REFERENCES categories(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_categories_parent ON categories(parent_id);
