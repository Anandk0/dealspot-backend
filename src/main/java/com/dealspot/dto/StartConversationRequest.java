package com.dealspot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StartConversationRequest {
    @NotNull(message = "Listing ID is required")
    private Long listingId;

    @NotBlank(message = "Message is required")
    private String message;
}
