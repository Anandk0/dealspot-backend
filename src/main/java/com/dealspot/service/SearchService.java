package com.dealspot.service;

import com.dealspot.dto.ListingResponse;
import com.dealspot.entity.Listing;
import com.dealspot.repository.ListingRepository;
import com.dealspot.util.PaginationUtil;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final ListingRepository listingRepository;

    /**
     * Advanced search with filters, sort, and buyer district prioritization.
     * Applies AND logic for all filters. When no explicit district filter is set
     * but a buyerDistrict is provided, results are ordered with local listings first.
     */
    public Page<ListingResponse> search(String query, String category, String district,
                                         Double priceMin, Double priceMax, String sort,
                                         String buyerDistrict, int page, int size) {

        Specification<Listing> spec = buildSearchSpecification(query, category, district, priceMin, priceMax, buyerDistrict);

        Pageable pageable;
        boolean hasBuyerDistrictPriority = (district == null || district.isBlank()) && buyerDistrict != null && !buyerDistrict.isBlank();

        if (hasBuyerDistrictPriority) {
            // When buyer has a district but no explicit district filter,
            // we use a custom specification with district priority ordering
            pageable = PaginationUtil.createPageable(page, size);
            return listingRepository.findAll(
                    buildSearchSpecificationWithDistrictPriority(query, category, priceMin, priceMax, buyerDistrict, sort),
                    pageable
            ).map(ListingResponse::fromEntity);
        }

        Sort jpaSort = resolveSort(sort);
        pageable = PaginationUtil.createPageable(page, size, jpaSort);
        return listingRepository.findAll(spec, pageable).map(ListingResponse::fromEntity);
    }

    /**
     * Get listings matching a specific district with ACTIVE status.
     */
    public Page<ListingResponse> getNearbyListings(String district, int page, int size) {
        Pageable pageable = PaginationUtil.createPageable(page, size, Sort.by("createdAt").descending());
        return listingRepository.findByDistrictAndStatus(district, "ACTIVE", pageable)
                .map(ListingResponse::fromEntity);
    }

    /**
     * Get featured listings with ACTIVE status.
     */
    public Page<ListingResponse> getFeaturedListings(int page, int size) {
        Pageable pageable = PaginationUtil.createPageable(page, size, Sort.by("createdAt").descending());
        return listingRepository.findByFeaturedTrueAndStatus("ACTIVE", pageable)
                .map(ListingResponse::fromEntity);
    }

    /**
     * Get trending listings — ACTIVE listings from the last 7 days sorted by view_count DESC.
     */
    public Page<ListingResponse> getTrendingListings(int page, int size) {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        Pageable pageable = PaginationUtil.createPageable(page, size);
        return listingRepository.findTrendingListings(since, pageable)
                .map(ListingResponse::fromEntity);
    }

    /**
     * Get similar listings — same category + district as the given listing, max {@code limit} results.
     * Excludes the source listing itself.
     */
    public List<ListingResponse> getSimilarListings(Long listingId, int limit) {
        int cappedLimit = Math.min(limit, 4);
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Listing not found"));

        if (listing.getCategory() == null || listing.getDistrict() == null) {
            return List.of();
        }

        Pageable pageable = PageRequest.of(0, cappedLimit);
        return listingRepository.findSimilarListings(
                listing.getCategory(), listing.getDistrict(), listingId, pageable
        ).stream().map(ListingResponse::fromEntity).toList();
    }

    /**
     * Build a JPA Specification for search with AND logic on all filters.
     * Only returns ACTIVE listings.
     */
    private Specification<Listing> buildSearchSpecification(String query, String category,
                                                             String district, Double priceMin,
                                                             Double priceMax, String buyerDistrict) {
        return (Root<Listing> root, CriteriaQuery<?> cq, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter by ACTIVE status
            predicates.add(cb.equal(root.get("status"), "ACTIVE"));

            // Text query filter: title OR titleEn OR description (case insensitive)
            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                Predicate titleEnMatch = cb.like(cb.lower(root.get("titleEn")), pattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(titleMatch, titleEnMatch, descMatch));
            }

            // Category filter (exact match)
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            // District filter (exact match)
            if (district != null && !district.isBlank()) {
                predicates.add(cb.equal(root.get("district"), district));
            }

            // Price range filters
            if (priceMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), priceMin));
            }
            if (priceMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), priceMax));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Build a JPA Specification that applies district prioritization in the ORDER BY clause.
     * When a buyer has a district set but no explicit district filter, results from the
     * buyer's district appear first, then results from other districts.
     */
    private Specification<Listing> buildSearchSpecificationWithDistrictPriority(
            String query, String category, Double priceMin, Double priceMax,
            String buyerDistrict, String sort) {
        return (Root<Listing> root, CriteriaQuery<?> cq, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter by ACTIVE status
            predicates.add(cb.equal(root.get("status"), "ACTIVE"));

            // Text query filter
            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                Predicate titleEnMatch = cb.like(cb.lower(root.get("titleEn")), pattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(titleMatch, titleEnMatch, descMatch));
            }

            // Category filter
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            // Price range filters
            if (priceMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), priceMin));
            }
            if (priceMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), priceMax));
            }

            // District prioritization in ORDER BY
            List<Order> orders = new ArrayList<>();
            // Primary sort: buyer's district first
            orders.add(cb.asc(cb.selectCase()
                    .when(cb.equal(root.get("district"), buyerDistrict), 0)
                    .otherwise(1)));

            // Secondary sort based on the sort parameter
            if (sort != null) {
                switch (sort) {
                    case "price_asc" -> orders.add(cb.asc(root.get("price")));
                    case "price_desc" -> orders.add(cb.desc(root.get("price")));
                    case "views" -> orders.add(cb.desc(root.get("viewCount")));
                    default -> orders.add(cb.desc(root.get("createdAt")));
                }
            } else {
                orders.add(cb.desc(root.get("createdAt")));
            }

            cq.orderBy(orders);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Resolve sort string to Spring Data Sort object.
     */
    private Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by("createdAt").descending();
        }
        return switch (sort) {
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            case "newest" -> Sort.by("createdAt").descending();
            case "views" -> Sort.by("viewCount").descending();
            default -> Sort.by("createdAt").descending();
        };
    }
}
