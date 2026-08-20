package com.dealspot.repository;

import com.dealspot.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    @Query("SELECT c FROM ChatConversation c WHERE c.buyer.id = :userId OR c.seller.id = :userId ORDER BY c.lastMessageAt DESC NULLS LAST")
    List<ChatConversation> findByBuyerIdOrSellerId(Long userId);

    Optional<ChatConversation> findByListingIdAndBuyerId(Long listingId, Long buyerId);
}
