package com.dealspot.service;

import com.dealspot.entity.*;
import com.dealspot.repository.*;
import com.dealspot.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OfferService {

    private final OfferRepository offerRepository;
    private final ListingRepository listingRepository;
    private final NotificationService notificationService;

    @Transactional
    public Offer makeOffer(Long listingId, Double amount, String message, User buyer) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        if (listing.getUser().getId().equals(buyer.getId())) {
            throw new RuntimeException("Cannot make offer on your own listing");
        }

        Offer offer = Offer.builder()
                .listing(listing)
                .buyer(buyer)
                .seller(listing.getUser())
                .amount(amount)
                .message(message)
                .build();
        offer = offerRepository.save(offer);

        // Notify seller
        notificationService.create(listing.getUser(),
                "ಹೊಸ ಬೆಲೆ ಆಫರ್",
                buyer.getName() + " ₹" + amount.intValue() + " ಆಫರ್ ಮಾಡಿದ್ದಾರೆ",
                "LISTING", "LISTING", listingId);

        return offer;
    }

    @Transactional
    public Offer respondToOffer(Long offerId, String action, User seller) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new RuntimeException("Offer not found"));

        if (!offer.getSeller().getId().equals(seller.getId())) {
            throw new RuntimeException("Not authorized");
        }

        if (!"PENDING".equals(offer.getStatus())) {
            throw new RuntimeException("Offer already responded to");
        }

        offer.setStatus(action.toUpperCase()); // ACCEPTED or REJECTED
        offer.setRespondedAt(LocalDateTime.now());
        offer = offerRepository.save(offer);

        // Notify buyer
        String msg = "ACCEPTED".equals(action.toUpperCase())
                ? "ನಿಮ್ಮ ₹" + offer.getAmount().intValue() + " ಆಫರ್ ಸ್ವೀಕರಿಸಲಾಗಿದೆ!"
                : "ನಿಮ್ಮ ₹" + offer.getAmount().intValue() + " ಆಫರ್ ನಿರಾಕರಿಸಲಾಗಿದೆ";
        notificationService.create(offer.getBuyer(), "ಆಫರ್ ಅಪ್ಡೇಟ್", msg, "LISTING", "LISTING", offer.getListing().getId());

        return offer;
    }

    public Page<Offer> getOffersForListing(Long listingId, int page, int size) {
        return offerRepository.findByListingIdOrderByCreatedAtDesc(listingId, PaginationUtil.createPageable(page, size));
    }

    public Page<Offer> getMyOffers(Long userId, int page, int size) {
        return offerRepository.findByBuyerIdOrderByCreatedAtDesc(userId, PaginationUtil.createPageable(page, size));
    }

    public Page<Offer> getPendingOffersForSeller(Long sellerId, int page, int size) {
        return offerRepository.findBySellerIdAndStatusOrderByCreatedAtDesc(sellerId, "PENDING", PaginationUtil.createPageable(page, size));
    }
}
