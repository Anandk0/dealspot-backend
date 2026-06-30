package com.dealspot.dto;

import com.dealspot.entity.Listing;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class ListingResponse {
    private Long id;
    private String title;
    private String titleEn;
    private String description;
    private String category;
    private Double price;
    private String priceUnit;
    private String location;
    private String district;
    private String status;
    private List<String> images;
    private Integer viewCount;
    private LocalDateTime createdAt;

    // Category-specific
    private String breed;
    private String age;
    private String condition;
    private String hp;
    private String area;
    private String skill;
    private String experience;
    private String vehicleType;
    private String rateInfo;

    // Seller info
    private Long sellerId;
    private String sellerName;
    private String sellerLocation;

    public static ListingResponse fromEntity(Listing listing) {
        return ListingResponse.builder()
                .id(listing.getId())
                .title(listing.getTitle())
                .titleEn(listing.getTitleEn())
                .description(listing.getDescription())
                .category(listing.getCategory())
                .price(listing.getPrice())
                .priceUnit(listing.getPriceUnit())
                .location(listing.getLocation())
                .district(listing.getDistrict())
                .status(listing.getStatus())
                .images(listing.getImages())
                .viewCount(listing.getViewCount())
                .createdAt(listing.getCreatedAt())
                .breed(listing.getBreed())
                .age(listing.getAge())
                .condition(listing.getCondition())
                .hp(listing.getHp())
                .area(listing.getArea())
                .skill(listing.getSkill())
                .experience(listing.getExperience())
                .vehicleType(listing.getVehicleType())
                .rateInfo(listing.getRateInfo())
                .sellerId(listing.getUser().getId())
                .sellerName(listing.getUser().getName())
                .sellerLocation(listing.getUser().getLocation())
                .build();
    }
}
