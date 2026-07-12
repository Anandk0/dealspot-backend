package com.dealspot.dto;

import com.dealspot.entity.Message;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @AllArgsConstructor
public class MessageResponse {
    private Long id;
    private Long senderId;
    private String senderName;
    private String content;
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
}
