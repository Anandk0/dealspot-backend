package com.dealspot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreateBannerRequest {
    @NotBlank(message = "Title is required")
    private String title;
    private String subtitle;
    private String imageUrl;
    private String link;
    private String color;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
