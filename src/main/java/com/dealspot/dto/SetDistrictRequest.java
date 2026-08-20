package com.dealspot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SetDistrictRequest {
    @NotBlank(message = "District is required")
    private String district;
}
