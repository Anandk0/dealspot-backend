package com.dealspot.controller;

import com.dealspot.dto.DistrictResponse;
import com.dealspot.dto.SetDistrictRequest;
import com.dealspot.entity.User;
import com.dealspot.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    /**
     * List all Karnataka districts (public, no auth needed).
     */
    @GetMapping("/districts")
    public ResponseEntity<List<DistrictResponse>> getDistricts() {
        List<String[]> districts = locationService.getDistricts();
        List<DistrictResponse> response = districts.stream()
                .map(d -> new DistrictResponse(
                        d[0].toLowerCase().replace(" ", "-"),  // id: slug from English name
                        d[1],                                   // name: Kannada
                        d[0],                                   // nameEn: English
                        "\uD83D\uDCCD"                          // icon: 📍 map pin emoji
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Set the authenticated user's preferred district.
     */
    @PutMapping("/users/me/district")
    public ResponseEntity<Map<String, String>> setUserDistrict(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SetDistrictRequest request) {
        locationService.setUserDistrict(user.getId(), request.getDistrict());
        return ResponseEntity.ok(Map.of(
                "message", "District updated successfully",
                "district", request.getDistrict()
        ));
    }
}
