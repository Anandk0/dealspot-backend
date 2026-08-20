package com.dealspot.service;

import com.dealspot.entity.*;
import com.dealspot.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ContactUnlockRepository contactUnlockRepository;

    /**
     * Submit a review for a seller.
     * Validates:
     * 1. Rating is between 1 and 5
     * 2. Buyer has not already reviewed this seller
     * 3. Buyer has unlocked the seller's contact (required before reviewing)
     */
    @Transactional
    public Review submitReview(Long buyerId, Long sellerId, int rating, String comment) {
        // Validate rating is between 1 and 5
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        // Validate buyer is not reviewing themselves
        if (buyerId.equals(sellerId)) {
            throw new RuntimeException("Cannot review yourself");
        }

        // Check for existing review (one review per buyer per seller)
        if (reviewRepository.existsByBuyerIdAndSellerId(buyerId, sellerId)) {
            throw new RuntimeException("Review already submitted for this seller");
        }

        // Check that buyer has unlocked the seller's contact
        boolean hasUnlock = contactUnlockRepository.existsByBuyerIdAndSellerId(buyerId, sellerId);
        if (!hasUnlock) {
            throw new RuntimeException("Contact unlock required before reviewing");
        }

        // Look up buyer and seller entities
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("Buyer not found"));
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        // Create and save the review
        Review review = Review.builder()
                .buyer(buyer)
                .seller(seller)
                .rating((short) rating)
                .comment(comment)
                .build();

        return reviewRepository.save(review);
    }

    /**
     * Get all reviews for a seller.
     */
    public List<Review> getSellerReviews(Long sellerId) {
        return reviewRepository.findBySellerId(sellerId);
    }

    /**
     * Compute the seller's average rating rounded to 1 decimal place.
     * Returns 0.0 if the seller has no reviews.
     */
    public Double getSellerAverageRating(Long sellerId) {
        Double avg = reviewRepository.averageRatingBySellerId(sellerId);
        if (avg == null || avg == 0.0) {
            return 0.0;
        }
        return Math.round(avg * 10) / 10.0;
    }

    /**
     * Get all reviews submitted by a buyer.
     */
    public List<Review> getMyReviews(Long buyerId) {
        return reviewRepository.findByBuyerId(buyerId);
    }
}
