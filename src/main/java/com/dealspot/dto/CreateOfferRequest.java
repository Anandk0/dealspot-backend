package com.dealspot.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateOfferRequest {
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;

    @Size(max = 500, message = "Message cannot exceed 500 characters")
    private String message;
}
