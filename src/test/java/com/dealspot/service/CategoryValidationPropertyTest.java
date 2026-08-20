package com.dealspot.service;

import com.dealspot.dto.CategoryRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import net.jqwik.api.*;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for CategoryRequest validation.
 *
 * Property 13: Missing Required Fields Rejected
 * Validates: Requirements 2.3
 *
 * Verifies that CategoryRequest with missing/blank name or nameEn
 * produces the expected constraint violations via Jakarta Bean Validation.
 */
@Tag("dynamic-categories")
class CategoryValidationPropertyTest {

    private static final Validator validator;

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    /**
     * Property: Any request with blank/null name and valid nameEn produces a violation on "name".
     *
     * Validates: Requirements 2.3
     */
    @Property(tries = 200)
    void blankName_withValidNameEn_producesNameViolation(
            @ForAll("blankStrings") String invalidName,
            @ForAll("validStrings") String validNameEn
    ) {
        CategoryRequest request = new CategoryRequest();
        request.setName(invalidName);
        request.setNameEn(validNameEn);

        Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(request);
        Set<String> violatedFields = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertTrue(violatedFields.contains("name"),
                "Expected violation on 'name' for value: " + repr(invalidName));
        assertFalse(violatedFields.contains("nameEn"),
                "Should have no violation on 'nameEn' for valid value: " + validNameEn);
    }

    /**
     * Property: Any request with valid name and blank/null nameEn produces a violation on "nameEn".
     *
     * Validates: Requirements 2.3
     */
    @Property(tries = 200)
    void validName_withBlankNameEn_producesNameEnViolation(
            @ForAll("validStrings") String validName,
            @ForAll("blankStrings") String invalidNameEn
    ) {
        CategoryRequest request = new CategoryRequest();
        request.setName(validName);
        request.setNameEn(invalidNameEn);

        Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(request);
        Set<String> violatedFields = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertTrue(violatedFields.contains("nameEn"),
                "Expected violation on 'nameEn' for value: " + repr(invalidNameEn));
        assertFalse(violatedFields.contains("name"),
                "Should have no violation on 'name' for valid value: " + validName);
    }

    /**
     * Property: Any request with both blank/null name and nameEn produces violations on both fields.
     *
     * Validates: Requirements 2.3
     */
    @Property(tries = 200)
    void bothBlank_producesBothViolations(
            @ForAll("blankStrings") String invalidName,
            @ForAll("blankStrings") String invalidNameEn
    ) {
        CategoryRequest request = new CategoryRequest();
        request.setName(invalidName);
        request.setNameEn(invalidNameEn);

        Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(request);
        Set<String> violatedFields = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertTrue(violatedFields.contains("name"),
                "Expected violation on 'name' for value: " + repr(invalidName));
        assertTrue(violatedFields.contains("nameEn"),
                "Expected violation on 'nameEn' for value: " + repr(invalidNameEn));
    }

    /**
     * Property: Any request with both valid (non-blank) name and nameEn has no violations on those fields.
     *
     * Validates: Requirements 2.3
     */
    @Property(tries = 200)
    void bothValid_producesNoViolationsOnNameFields(
            @ForAll("validStrings") String validName,
            @ForAll("validStrings") String validNameEn
    ) {
        CategoryRequest request = new CategoryRequest();
        request.setName(validName);
        request.setNameEn(validNameEn);

        Set<ConstraintViolation<CategoryRequest>> violations = validator.validate(request);
        Set<String> violatedFields = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertFalse(violatedFields.contains("name"),
                "Should have no violation on 'name' for valid value: " + validName);
        assertFalse(violatedFields.contains("nameEn"),
                "Should have no violation on 'nameEn' for valid value: " + validNameEn);
    }

    // ─── Providers ────────────────────────────────────────

    @Provide
    Arbitrary<String> blankStrings() {
        return Arbitraries.of(
                null,
                "",
                " ",
                "  ",
                "\t",
                "\n",
                " \t\n "
        );
    }

    @Provide
    Arbitrary<String> validStrings() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(50)
                .map(s -> s.trim().isEmpty() ? "a" : s);
    }

    // ─── Helpers ────────────────────────────────────────

    private static String repr(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\t", "\\t").replace("\n", "\\n") + "\"";
    }
}
