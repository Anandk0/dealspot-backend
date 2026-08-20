package com.dealspot.repository;

import com.dealspot.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findBySellerId(Long sellerId);

    List<Review> findByBuyerId(Long buyerId);

    boolean existsByBuyerIdAndSellerId(Long buyerId, Long sellerId);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.seller.id = :sellerId")
    Double averageRatingBySellerId(@Param("sellerId") Long sellerId);
}
