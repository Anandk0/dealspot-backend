package com.dealspot.dto;

import com.dealspot.entity.Offer;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @AllArgsConstructor
public class OfferResponse {
    private Long id;
    private Long listingId;
    private String listingTitle;
    private Long buyerId;
    private String buyerName;
    private Long sellerId;
    private Double amount;
    private String message;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    public static OfferResponse fromEntity(Offer o) {
        return OfferResponse.builder()
                .id(o.getId())
                .listingId(o.getListing().getId())
                .listingTitle(o.getListing().getTitle())
                .buyerId(o.getBuyer().getId())
                .buyerName(o.getBuyer().getName())
                .sellerId(o.getSeller().getId())
                .amount(o.getAmount())
                .message(o.getMessage())
                .status(o.getStatus())
                .createdAt(o.getCreatedAt())
                .respondedAt(o.getRespondedAt())
                .build();
    }
}
