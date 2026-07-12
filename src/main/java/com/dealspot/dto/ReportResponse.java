package com.dealspot.dto;

import com.dealspot.entity.Report;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @AllArgsConstructor
public class ReportResponse {
    private Long id;
    private Long reporterId;
    private String targetType;
    private Long targetId;
    private String reason;
    private String description;
    private String status;
    private LocalDateTime createdAt;

    public static ReportResponse fromEntity(Report r) {
        return ReportResponse.builder()
                .id(r.getId())
                .reporterId(r.getReporter().getId())
                .targetType(r.getTargetType())
                .targetId(r.getTargetId())
                .reason(r.getReason())
                .description(r.getDescription())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
