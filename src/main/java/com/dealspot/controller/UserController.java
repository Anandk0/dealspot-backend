package com.dealspot.controller;

import com.dealspot.dto.UserResponse;
import com.dealspot.entity.User;
import com.dealspot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> updates) {

        if (updates.containsKey("name")) user.setName(updates.get("name"));
        if (updates.containsKey("location")) user.setLocation(updates.get("location"));
        if (updates.containsKey("district")) user.setDistrict(updates.get("district"));
        if (updates.containsKey("email")) user.setEmail(updates.get("email"));

        user = userRepository.save(user);
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }
}
