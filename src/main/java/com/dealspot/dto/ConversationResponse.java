package com.dealspot.dto;

import com.dealspot.entity.ChatConversation;
import com.dealspot.entity.Conversation;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @AllArgsConstructor
public class ConversationResponse {
    private Long id;
    private Long listingId;
    private String listingTitle;
    private String listingImage;
    private Double listingPrice;
    private Long buyerId;
    private String buyerName;
    private Long sellerId;
    private String sellerName;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
    private Long unreadCount;

    public static ConversationResponse fromEntity(Conversation c) {
        return ConversationResponse.builder()
                .id(c.getId())
                .listingId(c.getListing().getId())
                .listingTitle(c.getListing().getTitle())
                .listingImage(c.getListing().getImages() != null && !c.getListing().getImages().isEmpty()
                        ? c.getListing().getImages().get(0) : null)
                .listingPrice(c.getListing().getPrice())
                .buyerId(c.getBuyer().getId())
                .buyerName(c.getBuyer().getName())
                .sellerId(c.getSeller().getId())
                .sellerName(c.getSeller().getName())
                .lastMessageAt(c.getLastMessageAt())
                .createdAt(c.getCreatedAt())
                .build();
    }

    public static ConversationResponse fromChatConversation(ChatConversation c) {
        return ConversationResponse.builder()
                .id(c.getId())
                .listingId(c.getListing().getId())
                .listingTitle(c.getListing().getTitle())
                .listingImage(c.getListing().getImages() != null && !c.getListing().getImages().isEmpty()
                        ? c.getListing().getImages().get(0) : null)
                .listingPrice(c.getListing().getPrice())
                .buyerId(c.getBuyer().getId())
                .buyerName(c.getBuyer().getName())
                .sellerId(c.getSeller().getId())
                .sellerName(c.getSeller().getName())
                .lastMessageAt(c.getLastMessageAt())
                .createdAt(c.getCreatedAt())
                .build();
    }

    public static ConversationResponse fromChatConversation(ChatConversation c, long unreadCount) {
        ConversationResponse response = fromChatConversation(c);
        response.setUnreadCount(unreadCount);
        return response;
    }
}
