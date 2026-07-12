package com.dealspot.controller;

import com.dealspot.dto.CreateOfferRequest;
import com.dealspot.dto.OfferResponse;
import com.dealspot.entity.User;
import com.dealspot.service.OfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    @PostMapping("/{listingId}")
    public ResponseEntity<OfferResponse> makeOffer(
            @PathVariable Long listingId,
            @Valid @RequestBody CreateOfferRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(OfferResponse.fromEntity(
                offerService.makeOffer(listingId, request.getAmount(), request.getMessage(), user)));
    }

    @PutMapping("/{offerId}/respond")
    public ResponseEntity<OfferResponse> respondToOffer(
            @PathVariable Long offerId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(OfferResponse.fromEntity(
                offerService.respondToOffer(offerId, body.get("action"), user)));
    }

    @GetMapping("/listing/{listingId}")
    public ResponseEntity<Page<OfferResponse>> getOffersForListing(
            @PathVariable Long listingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(offerService.getOffersForListing(listingId, page, size)
                .map(OfferResponse::fromEntity));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<OfferResponse>> getMyOffers(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(offerService.getMyOffers(user.getId(), page, size)
                .map(OfferResponse::fromEntity));
    }

    @GetMapping("/pending")
    public ResponseEntity<Page<OfferResponse>> getPendingOffers(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(offerService.getPendingOffersForSeller(user.getId(), page, size)
                .map(OfferResponse::fromEntity));
    }
}
