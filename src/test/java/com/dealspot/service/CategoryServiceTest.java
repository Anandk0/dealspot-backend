package com.dealspot.service;

import com.dealspot.entity.Category;
import com.dealspot.entity.ModerationLevel;
import com.dealspot.repository.CategoryRepository;
import com.dealspot.repository.ListingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private CategoryService categoryService;

    // ─── getActiveCategories ──────────────────────────────

    @Test
    void getActiveCategories_shouldReturnActiveCategoriesSortedBySortOrder() {
        Category cat1 = Category.builder().id(1L).name("ಆಸ್ತಿ").nameEn("Property").slug("property").sortOrder(1).active(true).build();
        Category cat2 = Category.builder().id(2L).name("ಕೃಷಿ").nameEn("Agriculture").slug("agriculture").sortOrder(2).active(true).build();

        when(categoryRepository.findByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of(cat1, cat2));

        List<Category> result = categoryService.getActiveCategories();

        assertEquals(2, result.size());
        assertEquals("property", result.get(0).getSlug());
        assertEquals("agriculture", result.get(1).getSlug());
        verify(categoryRepository).findByActiveTrueOrderBySortOrderAsc();
    }

    @Test
    void getActiveCategories_shouldReturnEmptyListWhenNoneActive() {
        when(categoryRepository.findByActiveTrueOrderBySortOrderAsc()).thenReturn(List.of());

        List<Category> result = categoryService.getActiveCategories();

        assertTrue(result.isEmpty());
    }

    // ─── getCategoryBySlug ────────────────────────────────

    @Test
    void getCategoryBySlug_shouldReturnCategoryWhenFound() {
        Category category = Category.builder().id(1L).name("ಆಸ್ತಿ").nameEn("Property").slug("property").build();
        when(categoryRepository.findBySlug("property")).thenReturn(Optional.of(category));

        Category result = categoryService.getCategoryBySlug("property");

        assertEquals("property", result.getSlug());
        assertEquals("Property", result.getNameEn());
    }

    @Test
    void getCategoryBySlug_shouldThrow404WhenNotFound() {
        when(categoryRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> categoryService.getCategoryBySlug("nonexistent"));

        assertEquals(404, exception.getStatusCode().value());
        assertTrue(exception.getReason().contains("Category not found"));
    }

    // ─── generateSlug ─────────────────────────────────────

    @Test
    void generateSlug_shouldConvertToLowercaseAndReplaceSpaces() {
        assertEquals("property-sale-rent", categoryService.generateSlug("Property Sale Rent"));
    }

    @Test
    void generateSlug_shouldReplaceSpecialCharsWithHyphens() {
        assertEquals("car-auto-rent", categoryService.generateSlug("Car & Auto Rent"));
    }

    @Test
    void generateSlug_shouldCollapseMultipleHyphens() {
        assertEquals("hello-world", categoryService.generateSlug("Hello---World"));
    }

    @Test
    void generateSlug_shouldTrimLeadingAndTrailingHyphens() {
        assertEquals("hello", categoryService.generateSlug("--hello--"));
    }

    @Test
    void generateSlug_shouldHandleEmptyString() {
        assertEquals("", categoryService.generateSlug(""));
    }

    @Test
    void generateSlug_shouldHandleNullInput() {
        assertEquals("", categoryService.generateSlug(null));
    }

    @Test
    void generateSlug_shouldHandleOnlySpecialChars() {
        assertEquals("", categoryService.generateSlug("@#$%^&"));
    }

    @Test
    void generateSlug_shouldBeDeterministic() {
        String input = "Agriculture Equipment";
        assertEquals(categoryService.generateSlug(input), categoryService.generateSlug(input));
    }

    @Test
    void generateSlug_shouldHandleUnicodeCharacters() {
        // Unicode chars get replaced with hyphens and collapsed
        assertEquals("test", categoryService.generateSlug("ಕೃಷಿ test"));
    }

    @Test
    void generateSlug_shouldPreserveExistingHyphens() {
        assertEquals("already-hyphenated", categoryService.generateSlug("already-hyphenated"));
    }
}
