package com.dealspot.repository;

import com.dealspot.entity.ContactUnlock;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ContactUnlockRepository extends JpaRepository<ContactUnlock, Long> {
    boolean existsByBuyerIdAndListingId(Long buyerId, Long listingId);
    Optional<ContactUnlock> findByBuyerIdAndListingId(Long buyerId, Long listingId);
    boolean existsByBuyerIdAndSellerId(Long buyerId, Long sellerId);
}
