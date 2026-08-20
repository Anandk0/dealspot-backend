package com.dealspot.dto;

import com.dealspot.entity.ModerationLevel;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CategorySettingsRequest {
    @NotNull(message = "Moderation level is required")
    private ModerationLevel moderationLevel;
}
