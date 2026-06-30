package com.dealspot.repository;

import com.dealspot.entity.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    Page<Listing> findByCategoryAndStatus(String category, String status, Pageable pageable);

    Page<Listing> findByUserIdAndStatus(Long userId, String status, Pageable pageable);

    Page<Listing> findByUserId(Long userId, Pageable pageable);

    Page<Listing> findByStatus(String status, Pageable pageable);

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
}
