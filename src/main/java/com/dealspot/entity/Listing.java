package com.dealspot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "listings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String titleEn;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String category;
    // agricultural-products, livestock, farm-equipment, tractor-rental,
    // vehicle-rental, labor, land, services

    private Double price;
    private String priceUnit; // per kg, per quintal, per hour, per day, etc.

    private String location;
    private String district;

    // Category-specific fields stored as JSON-like strings
    private String breed;       // livestock
    private String age;         // livestock, labor
    private String condition;   // equipment (new/used)
    private String hp;          // tractor
    private String area;        // land
    private String skill;       // labor
    private String experience;  // labor, services
    private String vehicleType; // vehicle
    private String rateInfo;    // tractor, vehicle, services

    @ElementCollection
    @CollectionTable(name = "listing_images", joinColumns = @JoinColumn(name = "listing_id"))
    @Column(name = "image_url")
    private List<String> images;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING"; // PENDING, ACTIVE, REJECTED, FLAGGED, SOLD, EXPIRED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @Builder.Default
    private Integer viewCount = 0;

    @Builder.Default
    private Boolean featured = false;

    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderated_by")
    private User moderatedBy;

    private LocalDateTime moderatedAt;

    private Double latitude;
    private Double longitude;
    private LocalDateTime expiresAt;

    @Builder.Default
    private Boolean promoted = false;

    private LocalDateTime promotedUntil;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
