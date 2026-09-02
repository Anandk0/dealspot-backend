package com.dealspot.service;

import com.dealspot.dto.ListingRequest;
import com.dealspot.dto.ListingResponse;
import com.dealspot.entity.Category;
import com.dealspot.entity.Listing;
import com.dealspot.entity.ModerationLevel;
import com.dealspot.entity.User;
import com.dealspot.repository.ListingRepository;
import com.dealspot.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;
    private final CloudinaryService cloudinaryService;
    private final CategoryService categoryService;

    @Transactional
    public ListingResponse createListing(ListingRequest request, List<MultipartFile> images, User user) {
        List<String> imageUrls = new ArrayList<>();
        if (images != null) {
            for (MultipartFile image : images) {
                if (!image.isEmpty()) {
                    imageUrls.add(cloudinaryService.uploadImageUrl(image));
                }
            }
        }

        // Determine initial status based on category moderation level
        String initialStatus = "PENDING";
        try {
            Category category = categoryService.getCategoryBySlug(request.getCategory());
            if (category.getModerationLevel() == ModerationLevel.NO_AUTH) {
                initialStatus = "ACTIVE";
            }
        } catch (Exception e) {
            // Category not found in DB, default to PENDING
        }

        Listing listing = Listing.builder()
                .title(request.getTitle())
                .titleEn(request.getTitleEn())
                .description(request.getDescription())
                .category(request.getCategory())
                .price(request.getPrice())
                .priceUnit(request.getPriceUnit())
                .location(request.getLocation())
                .district(request.getDistrict())
                .breed(request.getBreed())
                .age(request.getAge())
                .condition(request.getCondition())
                .hp(request.getHp())
                .area(request.getArea())
                .skill(request.getSkill())
                .experience(request.getExperience())
                .vehicleType(request.getVehicleType())
                .rateInfo(request.getRateInfo())
                .images(imageUrls)
                .status(initialStatus)
                .user(user)
                .build();

        listing = listingRepository.save(listing);
        return ListingResponse.fromEntity(listing);
    }

    public Page<ListingResponse> getListingsByCategory(String category, int page, int size) {
        Pageable pageable = PaginationUtil.createPageable(page, size, Sort.by("createdAt").descending());

        // Resolve category slug to include subcategory slugs if this is a parent category
        List<String> slugs = resolveCategorySlugs(category);

        if (slugs.size() == 1) {
            // Single slug — use the efficient derived query
            return listingRepository.findByCategoryAndStatus(slugs.get(0), "ACTIVE", pageable)
                    .map(ListingResponse::fromEntity);
        }
        // Parent with subcategories — use IN query
        return listingRepository.findByCategoryInAndStatus(slugs, "ACTIVE", pageable)
                .map(ListingResponse::fromEntity);
    }

    /**
     * Given a category slug, returns a list of slugs to query.
     * If the slug belongs to a parent category that has active children,
     * returns the parent slug PLUS all active child slugs.
     * If it's a leaf category (or not found), returns just the original slug.
     */
    private List<String> resolveCategorySlugs(String slug) {
        try {
            Category category = categoryService.getCategoryBySlug(slug);
            List<Category> children = categoryService.getActiveChildrenOf(category.getId());
            if (children.isEmpty()) {
                return List.of(slug);
            }
            List<String> slugs = new ArrayList<>();
            slugs.add(slug); // include parent slug too (listings posted directly to parent)
            children.forEach(c -> slugs.add(c.getSlug()));
            return slugs;
        } catch (Exception e) {
            return List.of(slug);
        }
    }

    public ListingResponse getListingById(Long id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found"));
        listing.setViewCount(listing.getViewCount() + 1);
        listingRepository.save(listing);
        return ListingResponse.fromEntity(listing);
    }

    public Page<ListingResponse> getMyListings(Long userId, int page, int size) {
        Pageable pageable = PaginationUtil.createPageable(page, size, Sort.by("createdAt").descending());
        return listingRepository.findByUserId(userId, pageable)
                .map(ListingResponse::fromEntity);
    }

    @Transactional
    public ListingResponse updateListing(Long id, ListingRequest request, List<MultipartFile> newImages, User user) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        if (!listing.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized to update this listing");
        }

        listing.setTitle(request.getTitle());
        listing.setTitleEn(request.getTitleEn());
        listing.setDescription(request.getDescription());
        listing.setCategory(request.getCategory());
        listing.setPrice(request.getPrice());
        listing.setPriceUnit(request.getPriceUnit());
        listing.setLocation(request.getLocation());
        listing.setDistrict(request.getDistrict());
        listing.setBreed(request.getBreed());
        listing.setAge(request.getAge());
        listing.setCondition(request.getCondition());
        listing.setHp(request.getHp());
        listing.setArea(request.getArea());
        listing.setSkill(request.getSkill());
        listing.setExperience(request.getExperience());
        listing.setVehicleType(request.getVehicleType());
        listing.setRateInfo(request.getRateInfo());

        if (newImages != null && !newImages.isEmpty()) {
            List<String> imageUrls = new ArrayList<>(listing.getImages() != null ? listing.getImages() : new ArrayList<>());
            for (MultipartFile image : newImages) {
                if (!image.isEmpty()) {
                    imageUrls.add(cloudinaryService.uploadImageUrl(image));
                }
            }
            listing.setImages(imageUrls);
        }

        listing = listingRepository.save(listing);
        return ListingResponse.fromEntity(listing);
    }

    @Transactional
    public void deleteListing(Long id, User user) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        if (!listing.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized to delete this listing");
        }

        // Delete images from Cloudinary
        if (listing.getImages() != null) {
            listing.getImages().forEach(url -> {
                // Extract public ID from URL and delete
                String publicId = extractPublicId(url);
                if (publicId != null) {
                    cloudinaryService.deleteByPublicId(publicId);
                }
            });
        }

        listingRepository.delete(listing);
    }

    private String extractPublicId(String url) {
        try {
            String[] parts = url.split("/upload/");
            if (parts.length > 1) {
                String path = parts[1].replaceFirst("v\\d+/", "");
                return path.substring(0, path.lastIndexOf('.'));
            }
        } catch (Exception e) { /* ignore */ }
        return null;
    }

    public Page<ListingResponse> search(String query, String category, int page, int size) {
        Pageable pageable = PaginationUtil.createPageable(page, size, Sort.by("createdAt").descending());
        if (category != null && !category.isBlank()) {
            return listingRepository.searchInCategory(query, category, pageable)
                    .map(ListingResponse::fromEntity);
        }
        return listingRepository.search(query, pageable)
                .map(ListingResponse::fromEntity);
    }

    public List<ListingResponse> getRecentListings() {
        return listingRepository.findTop10ByStatusOrderByCreatedAtDesc("ACTIVE")
                .stream().map(ListingResponse::fromEntity).toList();
    }

    public List<ListingResponse> getNearbyListings(double lat, double lng, double radiusKm) {
        return listingRepository.findNearby(lat, lng, radiusKm)
                .stream().map(ListingResponse::fromEntity).toList();
    }
}
