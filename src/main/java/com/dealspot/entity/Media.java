package com.dealspot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "media")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cloudinaryPublicId;

    @Column(nullable = false)
    private String cloudinaryUrl;

    @Column(nullable = false)
    private String secureUrl;

    private String fileName;
    private String fileType;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private String format;

    @Builder.Default
    private String resourceType = "image";

    private String folder;

    // Which entity this media belongs to
    private String entityType; // LISTING, USER_PROFILE, BANNER, CHAT
    private Long entityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
