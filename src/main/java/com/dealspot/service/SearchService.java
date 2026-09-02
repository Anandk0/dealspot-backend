package com.dealspot.service;

import com.dealspot.dto.ListingResponse;
import com.dealspot.entity.Category;
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
    private final CategoryService categoryService;

    /**
     * Advanced search with filters, sort, and buyer district prioritization.
     * When a parent category slug is given, automatically includes all subcategory slugs.
     */
    public Page<ListingResponse> search(String query, String category, String district,
                                         Double priceMin, Double priceMax, String sort,
                                         String buyerDistrict, int page, int size) {

        // Expand category slug to include subcategory slugs if applicable
        List<String> categorySlugs = resolveCategorySlugs(category);

        Specification<Listing> spec = buildSearchSpecification(query, categorySlugs, district, priceMin, priceMax, buyerDistrict);

        Pageable pageable;
        boolean hasBuyerDistrictPriority = (district == null || district.isBlank())
                && buyerDistrict != null && !buyerDistrict.isBlank();

        if (hasBuyerDistrictPriority) {
            pageable = PaginationUtil.createPageable(page, size);
            return listingRepository.findAll(
                    buildSearchSpecificationWithDistrictPriority(query, categorySlugs, priceMin, priceMax, buyerDistrict, sort),
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
     * Get similar listings — same category + district, max {@code limit} results.
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
     * Resolves a category slug to a list of slugs to filter by.
     * If the slug is a parent with active children, returns parent + all child slugs.
     * If null/blank or leaf, returns a list with just the original slug (or empty).
     */
    private List<String> resolveCategorySlugs(String category) {
        if (category == null || category.isBlank()) {
            return List.of();
        }
        try {
            Category cat = categoryService.getCategoryBySlug(category);
            List<Category> children = categoryService.getActiveChildrenOf(cat.getId());
            if (children.isEmpty()) {
                return List.of(category);
            }
            List<String> slugs = new ArrayList<>();
            slugs.add(category);
            children.forEach(c -> slugs.add(c.getSlug()));
            return slugs;
        } catch (Exception e) {
            return List.of(category);
        }
    }

    /**
     * Builds a category predicate: single equality or IN depending on slug count.
     */
    private Predicate buildCategoryPredicate(Root<Listing> root, CriteriaBuilder cb, List<String> slugs) {
        if (slugs.isEmpty()) return null;
        if (slugs.size() == 1) return cb.equal(root.get("category"), slugs.get(0));
        return root.get("category").in(slugs);
    }

    /**
     * Build a JPA Specification for search with AND logic on all filters.
     */
    private Specification<Listing> buildSearchSpecification(String query, List<String> categorySlugs,
                                                             String district, Double priceMin,
                                                             Double priceMax, String buyerDistrict) {
        return (Root<Listing> root, CriteriaQuery<?> cq, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("status"), "ACTIVE"));

            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("titleEn")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            Predicate catPredicate = buildCategoryPredicate(root, cb, categorySlugs);
            if (catPredicate != null) predicates.add(catPredicate);

            if (district != null && !district.isBlank()) {
                predicates.add(cb.equal(root.get("district"), district));
            }
            if (priceMin != null) predicates.add(cb.greaterThanOrEqualTo(root.get("price"), priceMin));
            if (priceMax != null) predicates.add(cb.lessThanOrEqualTo(root.get("price"), priceMax));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Build a JPA Specification with district prioritization in ORDER BY.
     */
    private Specification<Listing> buildSearchSpecificationWithDistrictPriority(
            String query, List<String> categorySlugs, Double priceMin, Double priceMax,
            String buyerDistrict, String sort) {
        return (Root<Listing> root, CriteriaQuery<?> cq, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("status"), "ACTIVE"));

            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("titleEn")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            Predicate catPredicate = buildCategoryPredicate(root, cb, categorySlugs);
            if (catPredicate != null) predicates.add(catPredicate);

            if (priceMin != null) predicates.add(cb.greaterThanOrEqualTo(root.get("price"), priceMin));
            if (priceMax != null) predicates.add(cb.lessThanOrEqualTo(root.get("price"), priceMax));

            List<Order> orders = new ArrayList<>();
            orders.add(cb.asc(cb.selectCase()
                    .when(cb.equal(root.get("district"), buyerDistrict), 0)
                    .otherwise(1)));

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
        if (sort == null || sort.isBlank()) return Sort.by("createdAt").descending();
        return switch (sort) {
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            case "newest" -> Sort.by("createdAt").descending();
            case "views" -> Sort.by("viewCount").descending();
            default -> Sort.by("createdAt").descending();
        };
    }
}
