package com.dealspot.repository;

import com.dealspot.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByActiveTrueOrderBySortOrderAsc();

    // Only top-level active categories (parent is null)
    List<Category> findByActiveTrueAndParentIsNullOrderBySortOrderAsc();

    // Active children of a given parent
    List<Category> findByParentIdAndActiveTrueOrderBySortOrderAsc(Long parentId);

    List<Category> findAllByOrderBySortOrderAsc();

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    // Count subcategories of a given parent (used before delete)
    long countByParentId(Long parentId);
}
