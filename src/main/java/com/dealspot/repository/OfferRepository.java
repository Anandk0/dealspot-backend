package com.dealspot.repository;

import com.dealspot.entity.Offer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferRepository extends JpaRepository<Offer, Long> {
    Page<Offer> findByListingIdOrderByCreatedAtDesc(Long listingId, Pageable pageable);
    Page<Offer> findBySellerIdAndStatusOrderByCreatedAtDesc(Long sellerId, String status, Pageable pageable);
    Page<Offer> findByBuyerIdOrderByCreatedAtDesc(Long buyerId, Pageable pageable);
}
