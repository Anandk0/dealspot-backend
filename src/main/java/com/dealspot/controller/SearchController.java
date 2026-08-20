package com.dealspot.controller;

import com.dealspot.dto.ListingResponse;
import com.dealspot.entity.User;
import com.dealspot.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * Enhanced search with query params: q, category, district, priceMin, priceMax, sort, page, size.
     * Public access. If the user is authenticated, their preferred_district is used for prioritization
     * when no explicit district filter is applied.
     */
    @GetMapping("/api/search")
    public ResponseEntity<Page<ListingResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Double priceMin,
            @RequestParam(required = false) Double priceMax,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {

        String buyerDistrict = (user != null) ? user.getPreferredDistrict() : null;

        return ResponseEntity.ok(
                searchService.search(q, category, district, priceMin, priceMax, sort, buyerDistrict, page, size)
        );
    }

    /**
     * Nearby listings by district.
     * If no district param is provided, uses the authenticated user's preferred_district.
     */
    @GetMapping("/api/listings/nearby")
    public ResponseEntity<Page<ListingResponse>> getNearbyListings(
            @RequestParam(required = false) String district,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User user) {

        String resolvedDistrict = district;
        if ((resolvedDistrict == null || resolvedDistrict.isBlank()) && user != null) {
            resolvedDistrict = user.getPreferredDistrict();
        }

        if (resolvedDistrict == null || resolvedDistrict.isBlank()) {
            return ResponseEntity.ok(Page.empty());
        }

        return ResponseEntity.ok(searchService.getNearbyListings(resolvedDistrict, page, size));
    }

    /**
     * Featured listings — admin-featured listings with ACTIVE status.
     */
    @GetMapping("/api/listings/featured")
    public ResponseEntity<Page<ListingResponse>> getFeaturedListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(searchService.getFeaturedListings(page, size));
    }

    /**
     * Trending listings — ACTIVE listings from the last 7 days sorted by view_count DESC.
     */
    @GetMapping("/api/listings/trending")
    public ResponseEntity<Page<ListingResponse>> getTrendingListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(searchService.getTrendingListings(page, size));
    }

    /**
     * Similar listings — same category + district as the given listing, max 4 results.
     */
    @GetMapping("/api/listings/{id}/similar")
    public ResponseEntity<List<ListingResponse>> getSimilarListings(
            @PathVariable Long id,
            @RequestParam(defaultValue = "4") int limit) {

        return ResponseEntity.ok(searchService.getSimilarListings(id, limit));
    }
}
