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

    // Optional precise coordinates captured from the device at post time.
    private Double latitude;
    private Double longitude;

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

    // Flexible per-category attributes as a JSON string (e.g. detailed property fields).
    private String details;
}
