-- Flexible per-category attributes stored as a JSON string (TEXT).
-- Used first by the detailed Property form (sales + rent): taluk, area, price
-- per unit, negotiable, road facing, utilities, documents, and the private
-- contact + map-location block (revealed only after contact unlock).
-- Stored as TEXT (not jsonb) since the app parses JSON itself and runs no
-- SQL JSON queries on it — avoids varchar->jsonb cast errors from Hibernate.
ALTER TABLE listings ADD COLUMN IF NOT EXISTS details TEXT;
