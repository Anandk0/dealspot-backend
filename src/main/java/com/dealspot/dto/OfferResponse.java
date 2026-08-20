package com.dealspot.dto;

import com.dealspot.entity.Offer;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @AllArgsConstructor
public class OfferResponse {
    private Long id;
    private Long listingId;
    private String listingTitle;
    private String listingImage;
    private Double listingPrice;
    private Long buyerId;
    private String buyerName;
    private Long sellerId;
    private BigDecimal amount;
    private String message;
    private String status;
    private BigDecimal counterAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OfferResponse fromEntity(Offer o) {
        List<String> images = o.getListing().getImages();
        String firstImage = (images != null && !images.isEmpty()) ? images.get(0) : null;

        return OfferResponse.builder()
                .id(o.getId())
                .listingId(o.getListing().getId())
                .listingTitle(o.getListing().getTitle())
                .listingImage(firstImage)
                .listingPrice(o.getListing().getPrice())
                .buyerId(o.getBuyer().getId())
                .buyerName(o.getBuyer().getName())
                .sellerId(o.getSeller().getId())
                .amount(o.getAmount())
                .message(o.getMessage())
                .status(o.getStatus().name())
                .counterAmount(o.getCounterAmount())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }
}
