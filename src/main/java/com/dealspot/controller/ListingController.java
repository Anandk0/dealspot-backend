package com.dealspot.controller;

import com.dealspot.dto.ListingRequest;
import com.dealspot.dto.ListingResponse;
import com.dealspot.entity.User;
import com.dealspot.service.ListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ListingResponse> createListing(
            @Valid @RequestPart("data") ListingRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(listingService.createListing(request, images, user));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<Page<ListingResponse>> getByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(listingService.getListingsByCategory(category, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(listingService.getListingById(id));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<ListingResponse>> getMyListings(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(listingService.getMyListings(user.getId(), page, size));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ListingResponse> updateListing(
            @PathVariable Long id,
            @Valid @RequestPart("data") ListingRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(listingService.updateListing(id, request, images, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteListing(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        listingService.deleteListing(id, user);
        return ResponseEntity.ok(Map.of("message", "Listing deleted successfully"));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<ListingResponse>> getRecentListings() {
        return ResponseEntity.ok(listingService.getRecentListings());
    }
}
