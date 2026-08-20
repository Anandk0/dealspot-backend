package com.dealspot.service;

import com.dealspot.entity.*;
import com.dealspot.repository.*;
import com.dealspot.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OfferService {

    private final OfferRepository offerRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // ===== New methods required by task 3.2 =====

    /**
     * Creates an offer from a buyer on a listing.
     * Validates:
     * - No existing PENDING offer for this buyer + listing
     * - Amount > 0 and ≤ 2× listing price
     * - Buyer is not the listing owner
     */
    @Transactional
    public Offer createOffer(Long buyerId, Long listingId, BigDecimal amount, String message) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Listing not found"));

        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new IllegalArgumentException("Buyer not found"));

        // Validate buyer is not the listing owner
        if (listing.getUser().getId().equals(buyerId)) {
            throw new IllegalArgumentException("Cannot make offer on your own listing");
        }

        // Validate no existing PENDING offer
        offerRepository.findByBuyerIdAndListingIdAndStatus(buyerId, listingId, OfferStatus.PENDING)
                .ifPresent(existing -> {
                    throw new IllegalStateException("Active offer already exists for this listing");
                });

        // Validate amount > 0
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Offer amount must be positive");
        }

        // Validate amount ≤ 2× listing price
        if (listing.getPrice() != null) {
            BigDecimal maxAmount = BigDecimal.valueOf(listing.getPrice()).multiply(BigDecimal.valueOf(2));
            if (amount.compareTo(maxAmount) > 0) {
                throw new IllegalArgumentException("Offer amount cannot exceed 2x the listing price");
            }
        }

        Offer offer = Offer.builder()
                .listing(listing)
                .buyer(buyer)
                .seller(listing.getUser())
                .amount(amount)
                .message(message)
                .status(OfferStatus.PENDING)
                .build();
        offer = offerRepository.save(offer);

        // Notify seller
        notificationService.create(listing.getUser(),
                "ಹೊಸ ಬೆಲೆ ಆಫರ್",
                buyer.getName() + " ₹" + amount.intValue() + " ಆಫರ್ ಮಾಡಿದ್ದಾರೆ",
                "LISTING", "LISTING", listingId);

        return offer;
    }

    /**
     * Accepts a pending offer. Transitions PENDING → ACCEPTED.
     * Only the seller (listing owner) can accept.
     */
    @Transactional
    public Offer acceptOffer(Long offerId, Long sellerId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found"));

        if (!offer.getSeller().getId().equals(sellerId)) {
            throw new IllegalArgumentException("Not authorized to accept this offer");
        }

        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new IllegalStateException("Only PENDING offers can be accepted");
        }

        offer.setStatus(OfferStatus.ACCEPTED);
        offer.setUpdatedAt(LocalDateTime.now());
        offer = offerRepository.save(offer);

        // Notify buyer
        notificationService.create(offer.getBuyer(),
                "ಆಫರ್ ಅಪ್ಡೇಟ್",
                "ನಿಮ್ಮ ₹" + offer.getAmount().intValue() + " ಆಫರ್ ಸ್ವೀಕರಿಸಲಾಗಿದೆ!",
                "LISTING", "LISTING", offer.getListing().getId());

        return offer;
    }

    /**
     * Rejects a pending offer. Transitions PENDING → REJECTED or COUNTER.
     * If counterAmount is provided and non-null, transitions to COUNTER.
     * Only the seller (listing owner) can reject.
     */
    @Transactional
    public Offer rejectOffer(Long offerId, Long sellerId, BigDecimal counterAmount) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found"));

        if (!offer.getSeller().getId().equals(sellerId)) {
            throw new IllegalArgumentException("Not authorized to reject this offer");
        }

        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new IllegalStateException("Only PENDING offers can be rejected");
        }

        if (counterAmount != null && counterAmount.compareTo(BigDecimal.ZERO) > 0) {
            offer.setStatus(OfferStatus.COUNTER);
            offer.setCounterAmount(counterAmount);
        } else {
            offer.setStatus(OfferStatus.REJECTED);
        }
        offer.setUpdatedAt(LocalDateTime.now());
        offer = offerRepository.save(offer);

        // Notify buyer
        String msg = offer.getStatus() == OfferStatus.COUNTER
                ? "ಮಾರಾಟಗಾರ ₹" + counterAmount.intValue() + " ಪ್ರತಿ-ಆಫರ್ ಮಾಡಿದ್ದಾರೆ"
                : "ನಿಮ್ಮ ₹" + offer.getAmount().intValue() + " ಆಫರ್ ನಿರಾಕರಿಸಲಾಗಿದೆ";
        notificationService.create(offer.getBuyer(),
                "ಆಫರ್ ಅಪ್ಡೇಟ್", msg,
                "LISTING", "LISTING", offer.getListing().getId());

        return offer;
    }

    /**
     * Withdraws (deletes) a pending offer. Only the buyer who created the offer can withdraw.
     */
    @Transactional
    public void withdrawOffer(Long offerId, Long buyerId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new IllegalArgumentException("Offer not found"));

        if (!offer.getBuyer().getId().equals(buyerId)) {
            throw new IllegalArgumentException("Not authorized to withdraw this offer");
        }

        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new IllegalStateException("Only PENDING offers can be withdrawn");
        }

        offerRepository.delete(offer);
    }

    /**
     * Returns all offers made by the given buyer.
     */
    public List<Offer> getMyOffers(Long buyerId) {
        return offerRepository.findByBuyerId(buyerId);
    }

    /**
     * Returns the active (PENDING) offer for a buyer on a specific listing, if any.
     */
    public Optional<Offer> getActiveOfferForListing(Long buyerId, Long listingId) {
        return offerRepository.findByBuyerIdAndListingIdAndStatus(buyerId, listingId, OfferStatus.PENDING);
    }

    // ===== Legacy methods (used by existing OfferController) =====

    @Transactional
    public Offer makeOffer(Long listingId, BigDecimal amount, String message, User buyer) {
        return createOffer(buyer.getId(), listingId, amount, message);
    }

    @Transactional
    public Offer respondToOffer(Long offerId, String action, User seller) {
        if ("ACCEPTED".equalsIgnoreCase(action)) {
            return acceptOffer(offerId, seller.getId());
        } else if ("REJECTED".equalsIgnoreCase(action)) {
            return rejectOffer(offerId, seller.getId(), null);
        } else if ("COUNTER".equalsIgnoreCase(action)) {
            // For COUNTER through legacy API, treat as rejection without counter amount
            return rejectOffer(offerId, seller.getId(), null);
        }
        throw new IllegalArgumentException("Invalid action: " + action);
    }

    public Page<Offer> getOffersForListing(Long listingId, int page, int size) {
        return offerRepository.findByListingIdOrderByCreatedAtDesc(listingId, PaginationUtil.createPageable(page, size));
    }

    public Page<Offer> getMyOffers(Long userId, int page, int size) {
        return offerRepository.findByBuyerIdOrderByCreatedAtDesc(userId, PaginationUtil.createPageable(page, size));
    }

    public Page<Offer> getPendingOffersForSeller(Long sellerId, int page, int size) {
        return offerRepository.findBySellerIdAndStatusOrderByCreatedAtDesc(sellerId, OfferStatus.PENDING, PaginationUtil.createPageable(page, size));
    }
}
