package com.dealspot.controller;

import com.dealspot.dto.ListingResponse;
import com.dealspot.entity.User;
import com.dealspot.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/{listingId}")
    public ResponseEntity<Map<String, String>> addFavorite(
            @PathVariable Long listingId,
            @AuthenticationPrincipal User user) {
        favoriteService.addFavorite(listingId, user);
        return ResponseEntity.ok(Map.of("message", "Added to favorites"));
    }

    @DeleteMapping("/{listingId}")
    public ResponseEntity<Map<String, String>> removeFavorite(
            @PathVariable Long listingId,
            @AuthenticationPrincipal User user) {
        favoriteService.removeFavorite(listingId, user);
        return ResponseEntity.ok(Map.of("message", "Removed from favorites"));
    }

    @GetMapping
    public ResponseEntity<Page<ListingResponse>> getFavorites(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(favoriteService.getFavorites(user.getId(), page, size));
    }

    @GetMapping("/check/{listingId}")
    public ResponseEntity<Map<String, Boolean>> checkFavorite(
            @PathVariable Long listingId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("isFavorite", favoriteService.isFavorite(listingId, user.getId())));
    }
}
