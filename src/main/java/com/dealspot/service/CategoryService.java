package com.dealspot.service;

import com.dealspot.dto.CategoryAdminResponse;
import com.dealspot.dto.CategoryPublicResponse;
import com.dealspot.dto.CategoryRequest;
import com.dealspot.entity.Category;
import com.dealspot.entity.ModerationLevel;
import com.dealspot.entity.User;
import com.dealspot.repository.CategoryRepository;
import com.dealspot.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ListingRepository listingRepository;
    private final AuditService auditService;

    /**
     * Returns active subcategories (children) of a given parent category id.
     */
    public List<Category> getActiveChildrenOf(Long parentId) {
        return categoryRepository.findByParentIdAndActiveTrueOrderBySortOrderAsc(parentId);
    }

    /**
     * Returns all active TOP-LEVEL categories with their active subcategories nested.
     * Used by the public /api/categories endpoint.
     * *** Existing behaviour preserved — only top-level items returned at root level ***
     */
    public List<Category> getActiveCategories() {
        return categoryRepository.findByActiveTrueAndParentIsNullOrderBySortOrderAsc();
    }

    /**
     * Returns active categories as a nested public response tree.
     * Top-level categories contain their subcategories inline.
     */
    public List<CategoryPublicResponse> getActiveCategoriesTree() {
        List<Category> all = categoryRepository.findByActiveTrueOrderBySortOrderAsc();

        // Separate parents and children
        List<Category> topLevel = all.stream()
                .filter(c -> c.getParent() == null)
                .collect(Collectors.toList());

        Map<Long, List<Category>> childrenByParentId = all.stream()
                .filter(c -> c.getParent() != null)
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        return topLevel.stream().map(parent -> {
            CategoryPublicResponse dto = CategoryPublicResponse.fromEntity(parent);
            List<Category> children = childrenByParentId.getOrDefault(parent.getId(), new ArrayList<>());
            List<CategoryPublicResponse> subDtos = children.stream()
                    .map(CategoryPublicResponse::fromEntity)
                    .collect(Collectors.toList());
            dto.setSubcategories(subDtos);
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Looks up a category by its slug.
     * Throws 404 if the slug does not match any category.
     */
    public Category getCategoryBySlug(String slug) {
        return categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Category not found"));
    }

    /**
     * Returns ALL categories (active + inactive) as a nested tree for admin view.
     * Top-level categories include their subcategories with listing counts.
     */
    public List<CategoryAdminResponse> getAllCategoriesAdmin() {
        List<Category> all = categoryRepository.findAllByOrderBySortOrderAsc();

        // Separate top-level and subcategories
        List<Category> topLevel = all.stream()
                .filter(c -> c.getParent() == null)
                .collect(Collectors.toList());

        Map<Long, List<Category>> childrenByParentId = all.stream()
                .filter(c -> c.getParent() != null)
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        return topLevel.stream().map(parent -> {
            long parentListingCount = listingRepository.countByCategory(parent.getSlug());
            CategoryAdminResponse dto = CategoryAdminResponse.fromEntity(parent, parentListingCount);

            List<Category> children = childrenByParentId.getOrDefault(parent.getId(), new ArrayList<>());
            List<CategoryAdminResponse> subDtos = children.stream().map(child -> {
                long childListingCount = listingRepository.countByCategory(child.getSlug());
                return CategoryAdminResponse.fromEntity(child, childListingCount);
            }).collect(Collectors.toList());

            dto.setSubcategories(subDtos);
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Creates a new category. Auto-generates slug from nameEn if not provided.
     * Validates slug uniqueness (throws 409 on conflict).
     * If request.parentId is set, links the category as a subcategory.
     */
    @Transactional
    public Category createCategory(CategoryRequest request, User actor) {
        String slug = request.getSlug();
        if (slug == null || slug.isBlank()) {
            slug = generateSlug(request.getNameEn());
        }

        if (categoryRepository.existsBySlug(slug)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A category with this slug already exists");
        }

        // Resolve parent if parentId provided
        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Parent category not found"));
        }

        Category category = Category.builder()
                .name(request.getName())
                .nameEn(request.getNameEn())
                .slug(slug)
                .icon(request.getIcon())
                .imageUrl(request.getImageUrl())
                .color(request.getColor())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .active(request.getActive() != null ? request.getActive() : true)
                .moderationLevel(request.getModerationLevel() != null
                        ? request.getModerationLevel() : ModerationLevel.CHECKER_ONLY)
                .parent(parent)
                .build();

        category = categoryRepository.save(category);

        auditService.audit(actor, "CREATE_CATEGORY", "CATEGORY", category.getId(),
                "Created category: " + category.getNameEn() + " (slug: " + category.getSlug() + ")"
                        + (parent != null ? " under parent: " + parent.getNameEn() : ""));

        return category;
    }

    /**
     * Updates an existing category. Validates slug uniqueness (excluding self).
     * If slug changes, cascades the update to all listings referencing the old slug.
     * If parentId changes, re-links the parent.
     */
    @Transactional
    public Category updateCategory(Long id, CategoryRequest request, User actor) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Category not found"));

        String oldSlug = category.getSlug();
        String newSlug = request.getSlug();

        // If slug is provided and different, validate uniqueness
        if (newSlug != null && !newSlug.isBlank() && !newSlug.equals(oldSlug)) {
            if (categoryRepository.existsBySlugAndIdNot(newSlug, id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Another category already uses this slug");
            }
            category.setSlug(newSlug);
            // Cascade slug change to all listings referencing the old slug
            listingRepository.updateCategorySlug(oldSlug, newSlug);
        }

        // Update basic fields
        category.setName(request.getName());
        category.setNameEn(request.getNameEn());
        category.setIcon(request.getIcon());
        category.setImageUrl(request.getImageUrl());
        category.setColor(request.getColor());

        if (request.getSortOrder() != null) category.setSortOrder(request.getSortOrder());
        if (request.getActive() != null) category.setActive(request.getActive());
        if (request.getModerationLevel() != null) category.setModerationLevel(request.getModerationLevel());

        // Update parent — null means "make it a top-level category"
        if (request.getParentId() != null) {
            // Prevent circular reference — a category cannot be its own parent
            if (request.getParentId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A category cannot be its own parent");
            }
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Parent category not found"));
            category.setParent(parent);
        } else {
            // parentId explicitly sent as null → promote to top-level
            category.setParent(null);
        }

        category = categoryRepository.save(category);

        auditService.audit(actor, "UPDATE_CATEGORY", "CATEGORY", category.getId(),
                "Updated category: " + category.getNameEn() + " (slug: " + category.getSlug() + ")");

        return category;
    }

    /**
     * Deactivates a category (sets active=false). Category remains in database.
     */
    @Transactional
    public void deactivateCategory(Long id, User actor) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Category not found"));
        category.setActive(false);
        categoryRepository.save(category);
        auditService.audit(actor, "DEACTIVATE_CATEGORY", "CATEGORY", category.getId(),
                "Deactivated category: " + category.getNameEn());
    }

    /**
     * Activates a category (sets active=true).
     */
    @Transactional
    public void activateCategory(Long id, User actor) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Category not found"));
        category.setActive(true);
        categoryRepository.save(category);
        auditService.audit(actor, "ACTIVATE_CATEGORY", "CATEGORY", category.getId(),
                "Activated category: " + category.getNameEn());
    }

    /**
     * Deletes a category. Rejects deletion if the category has listings or subcategories.
     */
    @Transactional
    public void deleteCategory(Long id, User actor) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Category not found"));

        long listingCount = listingRepository.countByCategory(category.getSlug());
        if (listingCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete category with existing listings. Deactivate it instead.");
        }

        // Prevent deletion if it has subcategories
        long childCount = categoryRepository.countByParentId(id);
        if (childCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete category that has subcategories. Delete subcategories first.");
        }

        categoryRepository.delete(category);

        auditService.audit(actor, "DELETE_CATEGORY", "CATEGORY", category.getId(),
                "Deleted category: " + category.getNameEn() + " (slug: " + category.getSlug() + ")");
    }

    /**
     * Updates the moderation level for a category.
     */
    @Transactional
    public void updateModerationLevel(Long id, ModerationLevel level, User actor) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Category not found"));
        ModerationLevel oldLevel = category.getModerationLevel();
        category.setModerationLevel(level);
        categoryRepository.save(category);
        auditService.audit(actor, "UPDATE_CATEGORY_MODERATION", "CATEGORY", category.getId(),
                "Changed moderation level from " + oldLevel + " to " + level
                        + " for category: " + category.getNameEn());
    }

    /**
     * Generates a URL-friendly slug from an English name.
     */
    public String generateSlug(String nameEn) {
        if (nameEn == null || nameEn.isBlank()) return "";
        return nameEn
                .toLowerCase()
                .replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
    }
}
