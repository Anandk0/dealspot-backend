package com.dealspot.dto;

import com.dealspot.entity.ModerationLevel;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "English name is required")
    private String nameEn;

    private String slug;
    private String icon;
    private String imageUrl;
    private String color;
    private Integer sortOrder;
    private Boolean active;
    private ModerationLevel moderationLevel;
}
