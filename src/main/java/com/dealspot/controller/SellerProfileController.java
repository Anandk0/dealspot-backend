package com.dealspot.controller;

import com.dealspot.dto.SellerProfileResponse;
import com.dealspot.service.SellerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sellers")
@RequiredArgsConstructor
public class SellerProfileController {

    private final SellerProfileService sellerProfileService;

    @GetMapping("/{id}/profile")
    public ResponseEntity<SellerProfileResponse> getSellerProfile(@PathVariable Long id) {
        return ResponseEntity.ok(sellerProfileService.getSellerProfile(id));
    }
}
