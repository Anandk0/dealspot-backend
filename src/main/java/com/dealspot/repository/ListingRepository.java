package com.dealspot.repository;

import com.dealspot.entity.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    Page<Listing> findByCategoryAndStatus(String category, String status, Pageable pageable);

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
}
