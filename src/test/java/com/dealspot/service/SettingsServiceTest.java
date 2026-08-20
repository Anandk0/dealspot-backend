package com.dealspot.service;

import com.dealspot.entity.Listing;
import com.dealspot.entity.PlatformSetting;
import com.dealspot.entity.User;
import com.dealspot.repository.ListingRepository;
import com.dealspot.repository.PlatformSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    @Mock private PlatformSettingRepository settingRepository;
    @Mock private ListingRepository listingRepository;
    @Mock private AuditService auditService;

    @InjectMocks private SettingsService settingsService;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = User.builder()
                .id(1L)
                .phone("9000000001")
                .name("Admin")
                .role("SUPER_ADMIN")
                .banned(false)
                .build();
    }

    // ─── VALID_SETTINGS constant ──────────────────────────

    @Test
    void validSettings_containsExpectedKeys() {
        assertTrue(SettingsService.VALID_SETTINGS.contains("contact_unlock_price"));
        assertTrue(SettingsService.VALID_SETTINGS.contains("max_images_per_listing"));
        assertTrue(SettingsService.VALID_SETTINGS.contains("listing_expiry_days"));
        assertTrue(SettingsService.VALID_SETTINGS.contains("maintenance_mode"));
        assertTrue(SettingsService.VALID_SETTINGS.contains("auto_approve_listings"));
        assertEquals(5, SettingsService.VALID_SETTINGS.size());
    }

    // ─── updateSetting: unknown key validation ────────────

    @Test
    void updateSetting_shouldThrow_forUnknownKey() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> settingsService.updateSetting("unknown_key", "value", admin));
        assertTrue(ex.getMessage().contains("Unknown setting key"));
    }

    @Test
    void updateSetting_shouldThrow_forArbitraryKey() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> settingsService.updateSetting("admin_password", "123", admin));
        assertTrue(ex.getMessage().contains("Unknown setting key"));
    }

    // ─── updateSetting: contact_unlock_price validation ───

    @Test
    void updateSetting_contactUnlockPrice_shouldAcceptPositiveInteger() {
        when(settingRepository.findById("contact_unlock_price"))
                .thenReturn(Optional.of(PlatformSetting.builder().key("contact_unlock_price").value("5000").build()));
        when(settingRepository.save(any(PlatformSetting.class))).thenAnswer(i -> i.getArgument(0));

        settingsService.updateSetting("contact_unlock_price", "7500", admin);

        verify(settingRepository).save(any(PlatformSetting.class));
        verify(auditService).audit(admin, "UPDATE_SETTING", "SETTING", null, "contact_unlock_price=7500");
    }

    @Test
    void updateSetting_contactUnlockPrice_shouldThrow_forNegative() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> settingsService.updateSetting("contact_unlock_price", "-100", admin));
        assertTrue(ex.getMessage().contains("positive integer"));
    }

    @Test
    void updateSetting_contactUnlockPrice_shouldThrow_forZero() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> settingsService.updateSetting("contact_unlock_price", "0", admin));
        assertTrue(ex.getMessage().contains("positive integer"));
    }

    @Test
    void updateSetting_contactUnlockPrice_shouldThrow_forNonNumeric() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> settingsService.updateSetting("contact_unlock_price", "abc", admin));
        assertTrue(ex.getMessage().contains("positive integer"));
    }

    // ─── updateSetting: max_images_per_listing validation ─

    @Test
    void updateSetting_maxImages_shouldAcceptValidRange() {
        when(settingRepository.findById("max_images_per_listing"))
                .thenReturn(Optional.of(PlatformSetting.builder().key("max_images_per_listing").value("5").build()));
        when(settingRepository.save(any(PlatformSetting.class))).thenAnswer(i -> i.getArgument(0));

        settingsService.updateSetting("max_images_per_listing", "10", admin);

        verify(settingRepository).save(any(PlatformSetting.class));
    }

    @Test
    void updateSetting_maxImages_shouldThrow_forZero() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> settingsService.updateSetting("max_images_per_listing", "0", admin));
        assertTrue(ex.getMessage().contains("between 1 and 20"));
    }

    @Test
    void updateSetting_maxImages_shouldThrow_forAbove20() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> settingsService.updateSetting("max_images_per_listing", "21", admin));
        assertTrue(ex.getMessage().contains("between 1 and 20"));
    }

    @Test
    void updateSetting_maxImages_shouldAcceptBoundary1() {
        when(settingRepository.findById("max_images_per_listing"))
                .thenReturn(Optional.of(PlatformSetting.builder().key("max_images_per_listing").value("5").build()));
        when(settingRepository.save(any(PlatformSetting.class))).thenAnswer(i -> i.getArgument(0));

        settingsService.updateSetting("max_images_per_listing", "1", admin);
        verify(settingRepository).save(any(PlatformSetting.class));
    }

    @Test
    void updateSetting_maxImages_shouldAcceptBoundary20() {
        when(settingRepository.findById("max_images_per_listing"))
                .thenReturn(Optional.of(PlatformSetting.builder().key("max_images_per_listing").value("5").build()));
        when(settingRepository.save(any(PlatformSetting.class))).thenAnswer(i -> i.getArgument(0));

        settingsService.updateSetting("max_images_per_listing", "20", admin);
        verify(settingRepository).save(any(PlatformSetting.class));
    }

    // ─── updateSetting: listing_expiry_days validation ────

    @Test
    void updateSetting_listingExpiryDays_shouldAcceptValidRange() {
        when(settingRepository.findById("listing_expiry_days"))
                .thenReturn(Optional.of(PlatformSetting.builder().key("listing_expiry_days").value("30").build()));
        when(settingRepository.save(any(PlatformSetting.class))).thenAnswer(i -> i.getArgument(0));

        settingsService.updateSetting("listing_expiry_days", "60", admin);

        verify(settingRepository).save(any(PlatformSetting.class));
    }

    @Test
    void updateSetting_listingExpiryDays_shouldThrow_forZero() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> settingsService.updateSetting("listing_expiry_days", "0", admin));
        assertTrue(ex.getMessage().contains("between 1 and 365"));
    }

    @Test
    void updateSetting_listingExpiryDays_shouldThrow_forAbove365() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> settingsService.updateSetting("listing_expiry_days", "400", admin));
        assertTrue(ex.getMessage().contains("between 1 and 365"));
    }

    // ─── updateSetting: maintenance_mode validation ───────

    @Test
    void updateSetting_maintenanceMode_shouldAcceptTrue() {
        when(settingRepository.findById("maintenance_mode"))
                .thenReturn(Optional.of(PlatformSetting.builder().key("maintenance_mode").value("false").build()));
        when(settingRepository.save(any(PlatformSetting.class))).thenAnswer(i -> i.getArgument(0));

        settingsService.updateSetting("maintenance_mode", "true", admin);

        verify(settingRepository).save(any(PlatformSetting.class));
    }

    @Test
    void updateSetting_maintenanceMode_shouldAcceptFalse() {
        when(settingRepository.findById("maintenance_mode"))
                .thenReturn(Optional.of(PlatformSetting.builder().key("maintenance_mode").value("true").build()));
        when(settingRepository.save(any(PlatformSetting.class))).thenAnswer(i -> i.getArgument(0));

        settingsService.updateSetting("maintenance_mode", "false", admin);

        verify(settingRepository).save(any(PlatformSetting.class));
    }

    @Test
    void updateSetting_maintenanceMode_shouldThrow_forInvalid() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> settingsService.updateSetting("maintenance_mode", "yes", admin));
        assertTrue(ex.getMessage().contains("'true' or 'false'"));
    }

    @Test
    void updateSetting_maintenanceMode_shouldThrow_forCapitalTrue() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> settingsService.updateSetting("maintenance_mode", "True", admin));
        assertTrue(ex.getMessage().contains("'true' or 'false'"));
    }

    // ─── updateSetting: empty value validation ────────────

    @Test
    void updateSetting_shouldThrow_forEmptyValue() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> settingsService.updateSetting("contact_unlock_price", "", admin));
        assertTrue(ex.getMessage().contains("cannot be empty"));
    }

    @Test
    void updateSetting_shouldThrow_forNullValue() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> settingsService.updateSetting("contact_unlock_price", null, admin));
        assertTrue(ex.getMessage().contains("cannot be empty"));
    }

    // ─── updateSetting: audit logging ─────────────────────

    @Test
    void updateSetting_shouldCallAuditService() {
        when(settingRepository.findById("maintenance_mode"))
                .thenReturn(Optional.of(PlatformSetting.builder().key("maintenance_mode").value("false").build()));
        when(settingRepository.save(any(PlatformSetting.class))).thenAnswer(i -> i.getArgument(0));

        settingsService.updateSetting("maintenance_mode", "true", admin);

        verify(auditService).audit(admin, "UPDATE_SETTING", "SETTING", null, "maintenance_mode=true");
    }

    // ─── createPlatformListing ────────────────────────────

    @Test
    void createPlatformListing_shouldCreateActiveListingOwnedByAdmin() {
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> {
            Listing saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        Listing result = settingsService.createPlatformListing(
                "Platform Ad", "A promotional listing", "services", 0.0, admin);

        assertEquals("ACTIVE", result.getStatus());
        assertEquals(admin, result.getUser());
        assertTrue(result.getFeatured());
        assertTrue(result.getPromoted());
        assertEquals("Platform Ad", result.getTitle());
        assertEquals("services", result.getCategory());
    }

    @Test
    void createPlatformListing_shouldAuditTheCreation() {
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> {
            Listing saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        settingsService.createPlatformListing("Ad Title", "desc", "livestock", 500.0, admin);

        verify(auditService).audit(admin, "CREATE_PLATFORM_LISTING", "LISTING", 42L,
                "title=Ad Title, category=livestock");
    }

    // ─── getAllSettings ───────────────────────────────────

    @Test
    void getAllSettings_shouldReturnMapFromRepository() {
        when(settingRepository.findAll()).thenReturn(List.of(
                PlatformSetting.builder().key("contact_unlock_price").value("5000").build(),
                PlatformSetting.builder().key("maintenance_mode").value("false").build()
        ));

        Map<String, String> result = settingsService.getAllSettings();

        assertEquals(2, result.size());
        assertEquals("5000", result.get("contact_unlock_price"));
        assertEquals("false", result.get("maintenance_mode"));
    }
}
