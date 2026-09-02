package com.dealspot.repository;

import com.dealspot.entity.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface ListingRepository extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {

    Page<Listing> findByCategoryAndStatus(String category, String status, Pageable pageable);

    // Fetch listings for multiple category slugs (parent + subcategories)
    Page<Listing> findByCategoryInAndStatus(List<String> categories, String status, Pageable pageable);

    Page<Listing> findByUserIdAndStatus(Long userId, String status, Pageable pageable);

    Page<Listing> findByUserId(Long userId, Pageable pageable);

    Page<Listing> findByStatus(String status, Pageable pageable);

    long countByStatus(String status);

    @Query("SELECT l FROM Listing l WHERE l.status = 'ACTIVE' AND " +
           "(LOWER(l.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(l.titleEn) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(l.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(l.location) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Listing> search(@Param("query") String query, Pageable pageable);

    @Query("SELECT l FROM Listing l WHERE l.status = 'ACTIVE' AND l.category = :category AND " +
           "(LOWER(l.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(l.titleEn) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Listing> searchInCategory(@Param("query") String query, @Param("category") String category, Pageable pageable);

    List<Listing> findTop10ByStatusOrderByCreatedAtDesc(String status);

    // Location-based search (Haversine formula for distance in km)
    @Query(value = "SELECT * FROM listings l WHERE l.status = 'ACTIVE' " +
           "AND l.latitude IS NOT NULL AND l.longitude IS NOT NULL " +
           "AND (6371 * acos(cos(radians(:lat)) * cos(radians(l.latitude)) * " +
           "cos(radians(l.longitude) - radians(:lng)) + sin(radians(:lat)) * " +
           "sin(radians(l.latitude)))) < :radiusKm " +
           "ORDER BY (6371 * acos(cos(radians(:lat)) * cos(radians(l.latitude)) * " +
           "cos(radians(l.longitude) - radians(:lng)) + sin(radians(:lat)) * " +
           "sin(radians(l.latitude)))) ASC",
           nativeQuery = true)
    List<Listing> findNearby(@Param("lat") double lat, @Param("lng") double lng, @Param("radiusKm") double radiusKm);

    // Moderation stats queries
    long countByStatusAndModeratedAtGreaterThanEqual(String status, LocalDateTime since);

    // Analytics: count listings grouped by category
    @Query("SELECT l.category, COUNT(l) FROM Listing l GROUP BY l.category ORDER BY COUNT(l) DESC")
    List<Object[]> countGroupByCategory();

    // Analytics: count listings grouped by status
    @Query("SELECT l.status, COUNT(l) FROM Listing l GROUP BY l.status")
    List<Object[]> countGroupByStatus();

    // Category management: count listings by category slug
    long countByCategory(String category);

    // Category management: update category slug on all listings referencing old slug
    @Modifying
    @Query("UPDATE Listing l SET l.category = :newSlug WHERE l.category = :oldSlug")
    int updateCategorySlug(@Param("oldSlug") String oldSlug, @Param("newSlug") String newSlug);

    // Nearby listings by district
    Page<Listing> findByDistrictAndStatus(String district, String status, Pageable pageable);

    // Featured listings
    Page<Listing> findByFeaturedTrueAndStatus(String status, Pageable pageable);

    // Trending listings (created within a time window, sorted by viewCount)
    @Query("SELECT l FROM Listing l WHERE l.status = 'ACTIVE' AND l.createdAt >= :since ORDER BY l.viewCount DESC")
    Page<Listing> findTrendingListings(@Param("since") LocalDateTime since, Pageable pageable);

    // Similar listings (same category and district, excluding the source listing)
    @Query("SELECT l FROM Listing l WHERE l.status = 'ACTIVE' AND l.category = :category AND l.district = :district AND l.id <> :excludeId")
    List<Listing> findSimilarListings(@Param("category") String category, @Param("district") String district, @Param("excludeId") Long excludeId, Pageable pageable);

    // Search with district prioritization (buyer's district first, then others)
    @Query("SELECT l FROM Listing l WHERE l.status = 'ACTIVE' AND " +
           "(LOWER(l.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(l.titleEn) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(l.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY CASE WHEN l.district = :buyerDistrict THEN 0 ELSE 1 END, l.createdAt DESC")
    Page<Listing> searchWithDistrictPriority(@Param("query") String query, @Param("buyerDistrict") String buyerDistrict, Pageable pageable);
}
