package com.dealspot.controller;

import com.dealspot.dto.CreateOfferRequest;
import com.dealspot.dto.OfferResponse;
import com.dealspot.dto.RejectOfferRequest;
import com.dealspot.entity.Offer;
import com.dealspot.entity.User;
import com.dealspot.service.OfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    /**
     * Create a new offer on a listing.
     * POST /api/offers
     */
    @PostMapping
    public ResponseEntity<OfferResponse> createOffer(
            @Valid @RequestBody CreateOfferRequest request,
            @AuthenticationPrincipal User user) {
        Offer offer = offerService.createOffer(
                user.getId(),
                request.getListingId(),
                request.getAmount(),
                request.getMessage());
        return ResponseEntity.status(HttpStatus.CREATED).body(OfferResponse.fromEntity(offer));
    }

    /**
     * List all offers made by the authenticated buyer.
     * GET /api/offers/my
     */
    @GetMapping("/my")
    public ResponseEntity<List<OfferResponse>> getMyOffers(
            @AuthenticationPrincipal User user) {
        List<OfferResponse> offers = offerService.getMyOffers(user.getId())
                .stream()
                .map(OfferResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(offers);
    }

    /**
     * Get the active (PENDING) offer for a listing by the authenticated buyer.
     * GET /api/offers/listing/{listingId}
     */
    @GetMapping("/listing/{listingId}")
    public ResponseEntity<OfferResponse> getActiveOfferForListing(
            @PathVariable Long listingId,
            @AuthenticationPrincipal User user) {
        Optional<Offer> offer = offerService.getActiveOfferForListing(user.getId(), listingId);
        return offer.map(o -> ResponseEntity.ok(OfferResponse.fromEntity(o)))
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Seller accepts an offer.
     * PUT /api/offers/{id}/accept
     */
    @PutMapping("/{id}/accept")
    public ResponseEntity<OfferResponse> acceptOffer(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        Offer offer = offerService.acceptOffer(id, user.getId());
        return ResponseEntity.ok(OfferResponse.fromEntity(offer));
    }

    /**
     * Seller rejects an offer, optionally with a counter amount.
     * PUT /api/offers/{id}/reject
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<OfferResponse> rejectOffer(
            @PathVariable Long id,
            @RequestBody(required = false) RejectOfferRequest request,
            @AuthenticationPrincipal User user) {
        Offer offer = offerService.rejectOffer(
                id,
                user.getId(),
                request != null ? request.getCounterAmount() : null);
        return ResponseEntity.ok(OfferResponse.fromEntity(offer));
    }

    /**
     * Buyer withdraws a pending offer.
     * DELETE /api/offers/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> withdrawOffer(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        offerService.withdrawOffer(id, user.getId());
        return ResponseEntity.ok(Map.of("message", "Offer withdrawn successfully"));
    }
}
