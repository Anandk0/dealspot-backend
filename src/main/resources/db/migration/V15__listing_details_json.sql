-- Flexible per-category attributes stored as JSON.
-- Used first by the detailed Property form (sales + rent): taluk, area, price
-- per unit, negotiable, road facing, utilities, documents, and the private
-- contact + map-location block (revealed only after contact unlock).
ALTER TABLE listings ADD COLUMN IF NOT EXISTS details JSONB;
