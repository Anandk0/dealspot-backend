package com.dealspot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class SellerProfileResponse {
    private Long id;
    private String name;
    private String district;
    private LocalDateTime memberSince;
    private int totalListings;
    private Double averageRating;
    private int totalReviews;
    private List<ListingResponse> listings;
    private List<ReviewResponse> reviews;
}
