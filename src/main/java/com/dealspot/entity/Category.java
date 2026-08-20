package com.dealspot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;          // Kannada name

    @Column(nullable = false)
    private String nameEn;        // English name

    @Column(nullable = false, unique = true)
    private String slug;          // URL-friendly identifier

    private String icon;          // Emoji or icon identifier
    private String imageUrl;      // Optional category image
    private String color;         // CSS color class (e.g., "bg-blue-100")

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ModerationLevel moderationLevel = ModerationLevel.CHECKER_ONLY;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
