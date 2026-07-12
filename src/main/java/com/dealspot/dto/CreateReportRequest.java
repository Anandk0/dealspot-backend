package com.dealspot.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateReportRequest {
    @NotBlank(message = "Target type is required")
    private String targetType;

    @NotNull(message = "Target ID is required")
    private Long targetId;

    @NotBlank(message = "Reason is required")
    private String reason;

    @Size(max = 1000)
    private String description;
}
