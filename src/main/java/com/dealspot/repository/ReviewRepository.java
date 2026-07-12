package com.dealspot.repository;

import com.dealspot.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByTargetUserIdOrderByCreatedAtDesc(Long targetUserId, Pageable pageable);
    boolean existsByReviewerIdAndListingId(Long reviewerId, Long listingId);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.targetUser.id = :userId")
    double getAverageRatingForUser(Long userId);

    long countByTargetUserId(Long userId);
}
