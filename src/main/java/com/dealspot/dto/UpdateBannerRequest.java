package com.dealspot.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UpdateBannerRequest {
    private String title;
    private String subtitle;
    private String imageUrl;
    private String link;
    private String color;
    private Boolean active;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
