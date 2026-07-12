package com.dealspot.controller;

import com.dealspot.entity.Review;
import com.dealspot.entity.User;
import com.dealspot.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<Review> createReview(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User user) {
        Long targetUserId = ((Number) body.get("targetUserId")).longValue();
        Long listingId = body.get("listingId") != null ? ((Number) body.get("listingId")).longValue() : null;
        int rating = ((Number) body.get("rating")).intValue();
        String comment = (String) body.get("comment");

        return ResponseEntity.ok(reviewService.createReview(targetUserId, listingId, rating, comment, user));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<Review>> getUserReviews(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reviewService.getReviewsForUser(userId, page, size));
    }

    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<Map<String, Object>> getUserRatingSummary(@PathVariable Long userId) {
        return ResponseEntity.ok(reviewService.getUserRatingSummary(userId));
    }
}
