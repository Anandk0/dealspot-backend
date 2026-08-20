-- ═══════════════════════════════════════════════════════════
-- V10: Listing indexes for buyer discovery feed
-- Supports nearby (district), featured, and trending queries
-- ═══════════════════════════════════════════════════════════

-- Composite index for nearby listings query (filter by district + status)
CREATE INDEX IF NOT EXISTS idx_listings_district_status ON listings(district, status);

-- Drop existing partial index on featured (from V3) and replace with composite index
-- that supports featured feed queries filtering by both featured flag and status
DROP INDEX IF EXISTS idx_listings_featured;
CREATE INDEX idx_listings_featured ON listings(featured, status);

-- Composite index for trending listings query (sort by view_count, filter by created_at for 7-day window)
CREATE INDEX IF NOT EXISTS idx_listings_views_7d ON listings(view_count, created_at);
