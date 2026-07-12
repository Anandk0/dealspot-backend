package com.dealspot.dto;

import com.dealspot.entity.Notification;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private String type;
    private String referenceType;
    private Long referenceId;
    private Boolean read;
    private LocalDateTime createdAt;

    public static NotificationResponse fromEntity(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .referenceType(n.getReferenceType())
                .referenceId(n.getReferenceId())
                .read(n.getRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
