package com.dealspot.dto;

import com.dealspot.entity.Review;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Long buyerId;
    private String buyerName;
    private Long sellerId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    public static ReviewResponse fromEntity(Review r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .buyerId(r.getBuyer().getId())
                .buyerName(r.getBuyer().getName())
                .sellerId(r.getSeller().getId())
                .rating(r.getRating().intValue())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
