package com.dealspot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DistrictResponse {
    private String id;
    private String name;      // Kannada name
    private String nameEn;    // English name
    private String icon;      // Emoji icon
}
