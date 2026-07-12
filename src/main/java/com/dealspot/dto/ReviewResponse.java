package com.dealspot.dto;

import com.dealspot.entity.Review;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Long reviewerId;
    private String reviewerName;
    private Long targetUserId;
    private Long listingId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    public static ReviewResponse fromEntity(Review r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .reviewerId(r.getReviewer().getId())
                .reviewerName(r.getReviewer().getName())
                .targetUserId(r.getTargetUser().getId())
                .listingId(r.getListing() != null ? r.getListing().getId() : null)
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
