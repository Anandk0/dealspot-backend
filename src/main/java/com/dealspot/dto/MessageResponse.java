package com.dealspot.dto;

import com.dealspot.entity.ChatMessage;
import com.dealspot.entity.Message;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @AllArgsConstructor
public class MessageResponse {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderName;
    private String content;
    private String voiceUrl;
    private String messageType;
    private Boolean read;
    private LocalDateTime createdAt;

    public static MessageResponse fromEntity(Message m) {
        return MessageResponse.builder()
                .id(m.getId())
                .senderId(m.getSender().getId())
                .senderName(m.getSender().getName())
                .content(m.getContent())
                .read(m.getRead())
                .createdAt(m.getCreatedAt())
                .build();
    }

    public static MessageResponse fromChatMessage(ChatMessage m) {
        return MessageResponse.builder()
                .id(m.getId())
                .conversationId(m.getConversation().getId())
                .senderId(m.getSender().getId())
                .senderName(m.getSender().getName())
                .content(m.getContent())
                .voiceUrl(m.getVoiceUrl())
                .messageType(m.getMessageType().name())
                .read(m.getRead())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
