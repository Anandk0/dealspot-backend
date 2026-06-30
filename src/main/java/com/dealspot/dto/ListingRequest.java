package com.dealspot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ListingRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String titleEn;

    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    private Double price;
    private String priceUnit;
    private String location;
    private String district;

    // Category-specific fields
    private String breed;
    private String age;
    private String condition;
    private String hp;
    private String area;
    private String skill;
    private String experience;
    private String vehicleType;
    private String rateInfo;
}
