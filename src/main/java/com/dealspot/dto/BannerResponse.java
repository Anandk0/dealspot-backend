package com.dealspot.dto;

import com.dealspot.entity.Banner;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @AllArgsConstructor
public class BannerResponse {
    private Long id;
    private String title;
    private String subtitle;
    private String imageUrl;
    private String link;
    private String color;
    private Boolean active;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime createdAt;

    public static BannerResponse fromEntity(Banner b) {
        return BannerResponse.builder()
                .id(b.getId())
                .title(b.getTitle())
                .subtitle(b.getSubtitle())
                .imageUrl(b.getImageUrl())
                .link(b.getLink())
                .color(b.getColor())
                .active(b.getActive())
                .startDate(b.getStartDate())
                .endDate(b.getEndDate())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
