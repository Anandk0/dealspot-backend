package com.dealspot.service;

import com.dealspot.entity.*;
import com.dealspot.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final PlatformSettingRepository settingRepository;
    private final ListingRepository listingRepository;
    private final AuditService auditService;

    /**
     * Known valid setting keys. Only these keys can be updated.
     */
    public static final List<String> VALID_SETTINGS = List.of(
            "contact_unlock_price",
            "max_images_per_listing",
            "listing_expiry_days",
            "maintenance_mode"
    );

    public Map<String, String> getAllSettings() {
        Map<String, String> settings = new HashMap<>();
        settingRepository.findAll().forEach(s -> settings.put(s.getKey(), s.getValue()));
        return settings;
    }

    public void updateSetting(String key, String value, User actor) {
        // Validate known key
        if (!VALID_SETTINGS.contains(key)) {
            throw new IllegalArgumentException("Unknown setting key: " + key);
        }

        // Validate non-empty value
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Setting value cannot be empty");
        }

        // Per-key validation
        validateSettingValue(key, value);

        PlatformSetting setting = settingRepository.findById(key)
                .orElse(PlatformSetting.builder().key(key).build());
        setting.setValue(value);
        setting.setUpdatedBy(actor.getId());
        setting.setUpdatedAt(LocalDateTime.now());
        settingRepository.save(setting);

        auditService.audit(actor, "UPDATE_SETTING", "SETTING", null, key + "=" + value);
    }

    /**
     * Creates a platform listing (admin posts listing without seller phone).
     * The listing is immediately ACTIVE, featured, and promoted.
     */
    public Listing createPlatformListing(String title, String description, String category,
                                          Double price, User admin) {
        Listing listing = Listing.builder()
                .title(title)
                .description(description)
                .category(category)
                .price(price)
                .status("ACTIVE")
                .featured(true)
                .promoted(true)
                .user(admin)
                .createdAt(LocalDateTime.now())
                .build();

        Listing saved = listingRepository.save(listing);

        auditService.audit(admin, "CREATE_PLATFORM_LISTING", "LISTING", saved.getId(),
                "title=" + title + ", category=" + category);

        return saved;
    }

    private void validateSettingValue(String key, String value) {
        switch (key) {
            case "contact_unlock_price" -> {
                try {
                    int price = Integer.parseInt(value);
                    if (price <= 0) {
                        throw new IllegalArgumentException(
                                "contact_unlock_price must be a positive integer (in paise)");
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "contact_unlock_price must be a positive integer (in paise)");
                }
            }
            case "max_images_per_listing" -> {
                try {
                    int max = Integer.parseInt(value);
                    if (max < 1 || max > 20) {
                        throw new IllegalArgumentException(
                                "max_images_per_listing must be between 1 and 20");
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "max_images_per_listing must be between 1 and 20");
                }
            }
            case "listing_expiry_days" -> {
                try {
                    int days = Integer.parseInt(value);
                    if (days < 1 || days > 365) {
                        throw new IllegalArgumentException(
                                "listing_expiry_days must be between 1 and 365");
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "listing_expiry_days must be between 1 and 365");
                }
            }
            case "maintenance_mode" -> {
                if (!"true".equals(value) && !"false".equals(value)) {
                    throw new IllegalArgumentException(
                            "maintenance_mode must be 'true' or 'false'");
                }
            }
        }
    }
}
