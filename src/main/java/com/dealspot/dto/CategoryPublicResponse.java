package com.dealspot.dto;

import com.dealspot.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryPublicResponse {
    private Long id;
    private String name;
    private String nameEn;
    private String slug;
    private String icon;
    private String imageUrl;
    private String color;
    private Long parentId;                          // null = top-level
    @Builder.Default
    private List<CategoryPublicResponse> subcategories = Collections.emptyList();

    public static CategoryPublicResponse fromEntity(Category category) {
        return CategoryPublicResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .nameEn(category.getNameEn())
                .slug(category.getSlug())
                .icon(category.getIcon())
                .imageUrl(category.getImageUrl())
                .color(category.getColor())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .subcategories(Collections.emptyList())
                .build();
    }
}
