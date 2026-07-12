package com.dealspot.dto;

import com.dealspot.entity.Conversation;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @AllArgsConstructor
public class ConversationResponse {
    private Long id;
    private Long listingId;
    private String listingTitle;
    private Long buyerId;
    private String buyerName;
    private Long sellerId;
    private String sellerName;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;

    public static ConversationResponse fromEntity(Conversation c) {
        return ConversationResponse.builder()
                .id(c.getId())
                .listingId(c.getListing().getId())
                .listingTitle(c.getListing().getTitle())
                .buyerId(c.getBuyer().getId())
                .buyerName(c.getBuyer().getName())
                .sellerId(c.getSeller().getId())
                .sellerName(c.getSeller().getName())
                .lastMessageAt(c.getLastMessageAt())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
