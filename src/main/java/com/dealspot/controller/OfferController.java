package com.dealspot.controller;

import com.dealspot.entity.Offer;
import com.dealspot.entity.User;
import com.dealspot.service.OfferService;
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
    public ResponseEntity<Offer> makeOffer(
            @PathVariable Long listingId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User user) {
        Double amount = ((Number) body.get("amount")).doubleValue();
        String message = (String) body.get("message");
        return ResponseEntity.ok(offerService.makeOffer(listingId, amount, message, user));
    }

    @PutMapping("/{offerId}/respond")
    public ResponseEntity<Offer> respondToOffer(
            @PathVariable Long offerId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(offerService.respondToOffer(offerId, body.get("action"), user));
    }

    @GetMapping("/listing/{listingId}")
    public ResponseEntity<Page<Offer>> getOffersForListing(
            @PathVariable Long listingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(offerService.getOffersForListing(listingId, page, size));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<Offer>> getMyOffers(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(offerService.getMyOffers(user.getId(), page, size));
    }

    @GetMapping("/pending")
    public ResponseEntity<Page<Offer>> getPendingOffers(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(offerService.getPendingOffersForSeller(user.getId(), page, size));
    }
}
