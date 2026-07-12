package com.dealspot.controller;

import com.dealspot.dto.BannerResponse;
import com.dealspot.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
public class BannerController {

    private final AdminService adminService;

    @GetMapping("/active")
    public ResponseEntity<List<BannerResponse>> getActiveBanners() {
        return ResponseEntity.ok(adminService.getActiveBanners().stream()
                .map(BannerResponse::fromEntity)
                .toList());
    }
}
