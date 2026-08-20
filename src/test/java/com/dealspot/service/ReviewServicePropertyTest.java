package com.dealspot.service;

import com.dealspot.entity.*;
import com.dealspot.repository.*;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for ReviewService.
 *
 * Validates: Requirements 5.3, 6.1, 6.2, 6.3, 6.4
 */
@Tag("buyer-experience")
@Tag("review-service")
class ReviewServicePropertyTest {

    private ReviewRepository reviewRepository;
    private UserRepository userRepository;
    private ContactUnlockRepository contactUnlockRepository;
    private ReviewService reviewService;

    @BeforeTry
    void setUp() {
        this.reviewRepository = mock(ReviewRepository.class);
        this.userRepository = mock(UserRepository.class);
        this.contactUnlockRepository = mock(ContactUnlockRepository.class);
        this.reviewService = new ReviewService(reviewRepository, userRepository, contactUnlockRepository);
    }

    // ─── Property 11: Average rating calculation ──────────────────────────────────

    /**
     * Property 11: Average rating calculation.
     *
     * For any non-empty list of integer ratings (each between 1 and 5 inclusive),
     * the computed average rating SHALL equal the arithmetic mean rounded to one
     * decimal place.
     *
     * Validates: Requirements 5.3, 6.2
     */
    @Property(tries = 100)
    void averageRating_matchesArithmeticMean_roundedToOneDecimal(
            @ForAll("ratingLists") List<Short> ratings
    ) {
        Long sellerId = 1L;

        // Compute expected arithmetic mean rounded to 1 decimal
        double sum = 0.0;
        for (Short r : ratings) {
            sum += r;
        }
        double rawMean = sum / ratings.size();
        double expectedAvg = Math.round(rawMean * 10) / 10.0;

        // Mock the repository to return the raw arithmetic mean
        when(reviewRepository.averageRatingBySellerId(sellerId)).thenReturn(rawMean);

        // Act
        Double actualAvg = reviewService.getSellerAverageRating(sellerId);

        // Assert
        assertEquals(expectedAvg, actualAvg, 0.001,
                "Average rating for ratings=" + ratings + " should be " + expectedAvg + " but was " + actualAvg);
    }

    // ─── Property 12: Rating requires prior contact unlock ────────────────────────

    /**
     * Property 12: Rating requires prior contact unlock.
     *
     * For any buyer-seller pair where the buyer has NOT unlocked the seller's contact,
     * attempting to submit a review SHALL throw a RuntimeException.
     *
     * Validates: Requirements 6.1, 6.4
     */
    @Property(tries = 100)
    void submitReview_throwsException_whenNoContactUnlock(
            @ForAll("buyerIds") Long buyerId,
            @ForAll("sellerIds") Long sellerId,
            @ForAll("validRatings") int rating
    ) {
        // Ensure buyer != seller
        if (buyerId.equals(sellerId)) {
            sellerId = sellerId + 1000L;
        }

        // Mock: no existing review, no contact unlock
        when(reviewRepository.existsByBuyerIdAndSellerId(buyerId, sellerId)).thenReturn(false);
        when(contactUnlockRepository.existsByBuyerIdAndSellerId(buyerId, sellerId)).thenReturn(false);

        // Act & Assert: should throw because no unlock exists
        Long finalSellerId = sellerId;
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                reviewService.submitReview(buyerId, finalSellerId, rating, "Great seller"));

        assertEquals("Contact unlock required before reviewing", exception.getMessage());

        // Verify: review was never saved
        verify(reviewRepository, never()).save(any(Review.class));
    }

    // ─── Property 13: One review per buyer per seller ─────────────────────────────

    /**
     * Property 13: One review per buyer per seller.
     *
     * For any buyer-seller pair where a review already exists, attempting to submit
     * a second review SHALL throw a RuntimeException, leaving the original review
     * unchanged.
     *
     * Validates: Requirements 6.3
     */
    @Property(tries = 100)
    void submitReview_rejectsDuplicate_whenReviewAlreadyExists(
            @ForAll("buyerIds") Long buyerId,
            @ForAll("sellerIds") Long sellerId,
            @ForAll("validRatings") int rating
    ) {
        // Ensure buyer != seller
        if (buyerId.equals(sellerId)) {
            sellerId = sellerId + 1000L;
        }

        // Mock: review already exists for this buyer-seller pair
        when(reviewRepository.existsByBuyerIdAndSellerId(buyerId, sellerId)).thenReturn(true);
        // Mock: contact unlock exists (so we test the duplicate check specifically)
        when(contactUnlockRepository.existsByBuyerIdAndSellerId(buyerId, sellerId)).thenReturn(true);

        // Act & Assert: should throw because review already exists
        Long finalSellerId = sellerId;
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                reviewService.submitReview(buyerId, finalSellerId, rating, "Another review"));

        assertEquals("Review already submitted for this seller", exception.getMessage());

        // Verify: save was never called (original review unchanged)
        verify(reviewRepository, never()).save(any(Review.class));
    }

    // ─── Providers ────────────────────────────────────────────────────────────────

    @Provide
    Arbitrary<List<Short>> ratingLists() {
        return Arbitraries.shorts().between((short) 1, (short) 5)
                .list().ofMinSize(1).ofMaxSize(50);
    }

    @Provide
    Arbitrary<Long> buyerIds() {
        return Arbitraries.longs().between(1L, 500L);
    }

    @Provide
    Arbitrary<Long> sellerIds() {
        return Arbitraries.longs().between(501L, 1000L);
    }

    @Provide
    Arbitrary<Integer> validRatings() {
        return Arbitraries.integers().between(1, 5);
    }
}
