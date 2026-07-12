package com.dealspot.service;

import com.dealspot.entity.*;
import com.dealspot.repository.*;
import com.dealspot.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;

    @Transactional
    public Review createReview(Long targetUserId, Long listingId, int rating, String comment, User reviewer) {
        if (reviewer.getId().equals(targetUserId)) {
            throw new RuntimeException("Cannot review yourself");
        }
        if (listingId != null && reviewRepository.existsByReviewerIdAndListingId(reviewer.getId(), listingId)) {
            throw new RuntimeException("Already reviewed this listing");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Listing listing = listingId != null ? listingRepository.findById(listingId).orElse(null) : null;

        Review review = Review.builder()
                .reviewer(reviewer)
                .targetUser(targetUser)
                .listing(listing)
                .rating(rating)
                .comment(comment)
                .build();

        return reviewRepository.save(review);
    }

    public Page<Review> getReviewsForUser(Long userId, int page, int size) {
        return reviewRepository.findByTargetUserIdOrderByCreatedAtDesc(userId, PaginationUtil.createPageable(page, size));
    }

    public Map<String, Object> getUserRatingSummary(Long userId) {
        double avg = reviewRepository.getAverageRatingForUser(userId);
        long count = reviewRepository.countByTargetUserId(userId);
        return Map.of("averageRating", Math.round(avg * 10) / 10.0, "totalReviews", count);
    }
}
