package com.dealspot.controller;

import com.dealspot.dto.CreateReviewRequest;
import com.dealspot.dto.ReviewResponse;
import com.dealspot.entity.User;
import com.dealspot.service.ReviewService;
import jakarta.validation.Valid;
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
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ReviewResponse.fromEntity(
                reviewService.createReview(
                        request.getTargetUserId(),
                        request.getListingId(),
                        request.getRating(),
                        request.getComment(),
                        user)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<ReviewResponse>> getUserReviews(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reviewService.getReviewsForUser(userId, page, size)
                .map(ReviewResponse::fromEntity));
    }

    @GetMapping("/user/{userId}/summary")
    public ResponseEntity<Map<String, Object>> getUserRatingSummary(@PathVariable Long userId) {
        return ResponseEntity.ok(reviewService.getUserRatingSummary(userId));
    }
}
