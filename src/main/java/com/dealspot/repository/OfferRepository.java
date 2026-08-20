package com.dealspot.repository;

import com.dealspot.entity.Offer;
import com.dealspot.entity.OfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    Optional<Offer> findByBuyerIdAndListingIdAndStatus(Long buyerId, Long listingId, OfferStatus status);

    List<Offer> findByBuyerId(Long buyerId);

    List<Offer> findBySellerIdAndStatus(Long sellerId, OfferStatus status);

    Page<Offer> findByListingIdOrderByCreatedAtDesc(Long listingId, Pageable pageable);

    Page<Offer> findByBuyerIdOrderByCreatedAtDesc(Long buyerId, Pageable pageable);

    Page<Offer> findBySellerIdAndStatusOrderByCreatedAtDesc(Long sellerId, OfferStatus status, Pageable pageable);
}
