package com.dealspot.service;

import com.dealspot.dto.ListingResponse;
import com.dealspot.dto.ReviewResponse;
import com.dealspot.dto.SellerProfileResponse;
import com.dealspot.entity.Listing;
import com.dealspot.entity.Review;
import com.dealspot.entity.User;
import com.dealspot.repository.ListingRepository;
import com.dealspot.repository.ReviewRepository;
import com.dealspot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SellerProfileService {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final ReviewRepository reviewRepository;

    /**
     * Returns a seller's public profile including basic info, active listings,
     * average rating, and reviews.
     */
    public SellerProfileResponse getSellerProfile(Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        // Get active listings for this seller
        Page<Listing> activeListingsPage = listingRepository.findByUserIdAndStatus(
                sellerId, "ACTIVE",
                PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        List<ListingResponse> activeListings = activeListingsPage.getContent().stream()
                .map(ListingResponse::fromEntity)
                .collect(Collectors.toList());

        // Get average rating
        Double averageRating = reviewRepository.averageRatingBySellerId(sellerId);
        // Round to 1 decimal place
        if (averageRating != null && averageRating > 0) {
            averageRating = Math.round(averageRating * 10.0) / 10.0;
        }

        // Get reviews
        List<Review> reviews = reviewRepository.findBySellerId(sellerId);
        List<ReviewResponse> reviewResponses = reviews.stream()
                .map(ReviewResponse::fromEntity)
                .collect(Collectors.toList());

        return SellerProfileResponse.builder()
                .id(seller.getId())
                .name(seller.getName())
                .district(seller.getDistrict())
                .memberSince(seller.getCreatedAt())
                .totalListings(activeListings.size())
                .averageRating(averageRating)
                .totalReviews(reviewResponses.size())
                .listings(activeListings)
                .reviews(reviewResponses)
                .build();
    }
}
