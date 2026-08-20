package com.dealspot.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateOfferRequest {
    @NotNull(message = "Listing ID is required")
    private Long listingId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amount;

    @Size(max = 500, message = "Message cannot exceed 500 characters")
    private String message;
}
