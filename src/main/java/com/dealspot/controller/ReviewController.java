package com.dealspot.controller;

import com.dealspot.dto.CreateReviewRequest;
import com.dealspot.dto.ReviewResponse;
import com.dealspot.entity.User;
import com.dealspot.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ReviewResponse.fromEntity(
                reviewService.submitReview(
                        user.getId(),
                        request.getSellerId(),
                        request.getRating(),
                        request.getComment())));
    }

    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<ReviewResponse>> getSellerReviews(@PathVariable Long sellerId) {
        return ResponseEntity.ok(reviewService.getSellerReviews(sellerId)
                .stream()
                .map(ReviewResponse::fromEntity)
                .toList());
    }

    @GetMapping("/my")
    public ResponseEntity<List<ReviewResponse>> getMyReviews(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(reviewService.getMyReviews(user.getId())
                .stream()
                .map(ReviewResponse::fromEntity)
                .toList());
    }

    @GetMapping("/seller/{sellerId}/summary")
    public ResponseEntity<Map<String, Object>> getSellerRatingSummary(@PathVariable Long sellerId) {
        Double avg = reviewService.getSellerAverageRating(sellerId);
        List<?> reviews = reviewService.getSellerReviews(sellerId);
        return ResponseEntity.ok(Map.of(
                "averageRating", avg,
                "totalReviews", reviews.size()));
    }
}
