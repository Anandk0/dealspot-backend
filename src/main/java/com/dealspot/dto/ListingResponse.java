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
    private Double latitude;
    private Double longitude;
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

    // Flexible per-category attributes (JSON string). The "private" sub-object
    // (contact + exact map) is stripped out unless the viewer has unlocked.
    private String details;

    // Seller info
    private Long sellerId;
    private String sellerName;
    private String sellerLocation;

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    public static ListingResponse fromEntity(Listing listing) {
        // Default: locked view (no private details). Used by list/search feeds.
        return fromEntity(listing, false);
    }

    public static ListingResponse fromEntity(Listing listing, boolean unlocked) {
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
                .latitude(listing.getLatitude())
                .longitude(listing.getLongitude())
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
                .details(sanitizeDetails(listing.getDetails(), unlocked))
                .sellerId(listing.getUser().getId())
                .sellerName(listing.getUser().getName())
                .sellerLocation(listing.getUser().getLocation())
                .build();
    }

    /**
     * Returns the details JSON, removing the "private" block (contact + exact
     * map coordinates) unless the viewer has unlocked the listing. If parsing
     * fails or there's nothing private, the original string is returned.
     */
    private static String sanitizeDetails(String details, boolean unlocked) {
        if (details == null || details.isBlank() || unlocked) {
            return details;
        }
        try {
            com.fasterxml.jackson.databind.JsonNode node = MAPPER.readTree(details);
            if (node.has("private")) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) node).remove("private");
                return MAPPER.writeValueAsString(node);
            }
            return details;
        } catch (Exception e) {
            // Malformed JSON — safest is to drop it rather than leak private data.
            return null;
        }
    }
}
