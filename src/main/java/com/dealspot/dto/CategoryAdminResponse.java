package com.dealspot.dto;

import com.dealspot.entity.Category;
import com.dealspot.entity.ModerationLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryAdminResponse {
    private Long id;
    private String name;
    private String nameEn;
    private String slug;
    private String icon;
    private String imageUrl;
    private String color;
    private Boolean active;
    private Integer sortOrder;
    private ModerationLevel moderationLevel;
    private Long listingCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long parentId;                              // null = top-level
    private String parentName;                          // convenience display field
    @Builder.Default
    private List<CategoryAdminResponse> subcategories = Collections.emptyList();

    public static CategoryAdminResponse fromEntity(Category category, long listingCount) {
        return CategoryAdminResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .nameEn(category.getNameEn())
                .slug(category.getSlug())
                .icon(category.getIcon())
                .imageUrl(category.getImageUrl())
                .color(category.getColor())
                .active(category.getActive())
                .sortOrder(category.getSortOrder())
                .moderationLevel(category.getModerationLevel())
                .listingCount(listingCount)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .parentName(category.getParent() != null ? category.getParent().getName() : null)
                .subcategories(Collections.emptyList())
                .build();
    }
}
