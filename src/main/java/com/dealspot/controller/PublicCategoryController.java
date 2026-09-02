package com.dealspot.controller;

import com.dealspot.dto.CategoryPublicResponse;
import com.dealspot.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class PublicCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryPublicResponse>> getActiveCategories() {
        // Returns top-level categories with subcategories nested inside
        return ResponseEntity.ok(categoryService.getActiveCategoriesTree());
    }
}
