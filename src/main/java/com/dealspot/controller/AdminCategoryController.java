package com.dealspot.controller;

import com.dealspot.dto.CategoryAdminResponse;
import com.dealspot.dto.CategoryRequest;
import com.dealspot.dto.CategorySettingsRequest;
import com.dealspot.entity.Category;
import com.dealspot.entity.User;
import com.dealspot.repository.ListingRepository;
import com.dealspot.service.AdminService;
import com.dealspot.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;
    private final AdminService adminService;
    private final ListingRepository listingRepository;

    @GetMapping
    public ResponseEntity<List<CategoryAdminResponse>> getAllCategories(
            @AuthenticationPrincipal User user) {
        adminService.checkRole(user, "ADMIN", "SUPER_ADMIN");
        List<CategoryAdminResponse> categories = categoryService.getAllCategoriesAdmin();
        return ResponseEntity.ok(categories);
    }

    @PostMapping
    public ResponseEntity<CategoryAdminResponse> createCategory(
            @Valid @RequestBody CategoryRequest request,
            @AuthenticationPrincipal User user) {
        adminService.checkRole(user, "ADMIN", "SUPER_ADMIN");
        Category category = categoryService.createCategory(request, user);
        CategoryAdminResponse response = CategoryAdminResponse.fromEntity(category, 0);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryAdminResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request,
            @AuthenticationPrincipal User user) {
        adminService.checkRole(user, "ADMIN", "SUPER_ADMIN");
        Category category = categoryService.updateCategory(id, request, user);
        long listingCount = listingRepository.countByCategory(category.getSlug());
        CategoryAdminResponse response = CategoryAdminResponse.fromEntity(category, listingCount);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteCategory(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        adminService.checkRole(user, "ADMIN", "SUPER_ADMIN");
        categoryService.deleteCategory(id, user);
        return ResponseEntity.ok(Map.of("message", "Category deleted successfully"));
    }

    @PutMapping("/{id}/settings")
    public ResponseEntity<CategoryAdminResponse> updateSettings(
            @PathVariable Long id,
            @Valid @RequestBody CategorySettingsRequest request,
            @AuthenticationPrincipal User user) {
        adminService.checkRole(user, "ADMIN", "SUPER_ADMIN");
        categoryService.updateModerationLevel(id, request.getModerationLevel(), user);
        // Re-fetch the category to return updated state
        List<CategoryAdminResponse> allCategories = categoryService.getAllCategoriesAdmin();
        CategoryAdminResponse response = allCategories.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElseThrow();
        return ResponseEntity.ok(response);
    }
}
