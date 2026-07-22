package com.dealspot.dto;

import com.dealspot.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String phone;
    private String email;
    private String name;
    private String location;
    private String district;
    private String profileImage;
    private String role;
    private LocalDateTime createdAt;

    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .email(user.getEmail())
                .name(user.getName())
                .location(user.getLocation())
                .district(user.getDistrict())
                .profileImage(user.getProfileImage())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
