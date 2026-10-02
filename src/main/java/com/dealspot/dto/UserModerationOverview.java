package com.dealspot.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Consolidated moderation view of a listing's owner: their account details
 * (incl. ban status), the ads of theirs that have been reported, and all of
 * their posted ads. Assembled for the admin "reported ad" drill-down.
 */
@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class UserModerationOverview {

    private OwnerInfo owner;
    private List<ReportedListing> reportedListings;
    private List<ListingResponse> allListings;

    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class OwnerInfo {
        private Long id;
        private String name;
        private String phone;
        private String email;
        private String location;
        private String district;
        private String role;
        private Boolean banned;
        private String banReason;
        private LocalDateTime createdAt;
        private int totalListings;
        private int activeListings;
    }

    /** A listing plus the reports filed against it. */
    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class ReportedListing {
        private ListingResponse listing;
        private int reportCount;
        private List<String> reasons;
    }
}
