package com.dealspot.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "platform_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformSetting {

    @Id
    private String key;

    @Column(nullable = false)
    private String value;

    private Long updatedBy;

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
