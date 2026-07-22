package com.dealspot.service;

import com.dealspot.entity.Banner;
import com.dealspot.entity.User;
import com.dealspot.repository.BannerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BannerServiceTest {

    @Mock private BannerRepository bannerRepository;
    @Mock private AuditService auditService;

    @InjectMocks private BannerService bannerService;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = User.builder().id(1L).phone("9000000001").name("Admin").role("ADMIN").banned(false).build();
    }

    // ─── getActiveBanners date-range filtering ───────────────────

    @Test
    void getActiveBanners_shouldReturnBannersWithNoDates() {
        Banner bannerNoDate = Banner.builder()
                .id(1L).title("No Dates").active(true)
                .startDate(null).endDate(null).build();

        when(bannerRepository.findByActiveTrueOrderByCreatedAtDesc())
                .thenReturn(List.of(bannerNoDate));

        List<Banner> result = bannerService.getActiveBanners();
        assertEquals(1, result.size());
        assertEquals("No Dates", result.get(0).getTitle());
    }

    @Test
    void getActiveBanners_shouldReturnBannersWithinDateRange() {
        LocalDateTime now = LocalDateTime.now();
        Banner bannerInRange = Banner.builder()
                .id(2L).title("In Range").active(true)
                .startDate(now.minusDays(1)).endDate(now.plusDays(1)).build();

        when(bannerRepository.findByActiveTrueOrderByCreatedAtDesc())
                .thenReturn(List.of(bannerInRange));

        List<Banner> result = bannerService.getActiveBanners();
        assertEquals(1, result.size());
        assertEquals("In Range", result.get(0).getTitle());
    }

    @Test
    void getActiveBanners_shouldExcludeBannersBeforeStartDate() {
        LocalDateTime now = LocalDateTime.now();
        Banner bannerFuture = Banner.builder()
                .id(3L).title("Future Banner").active(true)
                .startDate(now.plusDays(1)).endDate(now.plusDays(10)).build();

        when(bannerRepository.findByActiveTrueOrderByCreatedAtDesc())
                .thenReturn(List.of(bannerFuture));

        List<Banner> result = bannerService.getActiveBanners();
        assertTrue(result.isEmpty());
    }

    @Test
    void getActiveBanners_shouldExcludeBannersAfterEndDate() {
        LocalDateTime now = LocalDateTime.now();
        Banner bannerExpired = Banner.builder()
                .id(4L).title("Expired Banner").active(true)
                .startDate(now.minusDays(10)).endDate(now.minusDays(1)).build();

        when(bannerRepository.findByActiveTrueOrderByCreatedAtDesc())
                .thenReturn(List.of(bannerExpired));

        List<Banner> result = bannerService.getActiveBanners();
        assertTrue(result.isEmpty());
    }

    @Test
    void getActiveBanners_shouldReturnBannersWithOnlyStartDateInPast() {
        LocalDateTime now = LocalDateTime.now();
        Banner bannerOnlyStart = Banner.builder()
                .id(5L).title("Only Start").active(true)
                .startDate(now.minusDays(1)).endDate(null).build();

        when(bannerRepository.findByActiveTrueOrderByCreatedAtDesc())
                .thenReturn(List.of(bannerOnlyStart));

        List<Banner> result = bannerService.getActiveBanners();
        assertEquals(1, result.size());
    }

    @Test
    void getActiveBanners_shouldReturnBannersWithOnlyEndDateInFuture() {
        LocalDateTime now = LocalDateTime.now();
        Banner bannerOnlyEnd = Banner.builder()
                .id(6L).title("Only End").active(true)
                .startDate(null).endDate(now.plusDays(1)).build();

        when(bannerRepository.findByActiveTrueOrderByCreatedAtDesc())
                .thenReturn(List.of(bannerOnlyEnd));

        List<Banner> result = bannerService.getActiveBanners();
        assertEquals(1, result.size());
    }

    @Test
    void getActiveBanners_shouldFilterMixedBanners() {
        LocalDateTime now = LocalDateTime.now();
        Banner valid = Banner.builder()
                .id(1L).title("Valid").active(true)
                .startDate(now.minusDays(1)).endDate(now.plusDays(1)).build();
        Banner expired = Banner.builder()
                .id(2L).title("Expired").active(true)
                .startDate(now.minusDays(10)).endDate(now.minusDays(1)).build();
        Banner future = Banner.builder()
                .id(3L).title("Future").active(true)
                .startDate(now.plusDays(1)).endDate(now.plusDays(10)).build();
        Banner noDate = Banner.builder()
                .id(4L).title("No Date").active(true)
                .startDate(null).endDate(null).build();

        when(bannerRepository.findByActiveTrueOrderByCreatedAtDesc())
                .thenReturn(Arrays.asList(valid, expired, future, noDate));

        List<Banner> result = bannerService.getActiveBanners();
        assertEquals(2, result.size());
        assertEquals("Valid", result.get(0).getTitle());
        assertEquals("No Date", result.get(1).getTitle());
    }

    // ─── updateBanner ────────────────────────────────────────────

    @Test
    void updateBanner_shouldUpdateTitle() {
        Banner existing = Banner.builder()
                .id(1L).title("Old Title").subtitle("Sub").active(true).build();
        Banner updatedFields = Banner.builder()
                .title("New Title").build();

        when(bannerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(bannerRepository.save(any(Banner.class))).thenReturn(existing);

        Banner result = bannerService.updateBanner(1L, updatedFields, admin);

        assertEquals("New Title", existing.getTitle());
        verify(auditService).audit(admin, "UPDATE_BANNER", "BANNER", 1L, "New Title");
    }

    @Test
    void updateBanner_shouldUpdateMultipleFields() {
        Banner existing = Banner.builder()
                .id(1L).title("Old").subtitle("Old Sub").color("#000").active(true).build();
        Banner updatedFields = Banner.builder()
                .title("New Title").subtitle("New Sub").color("#fff")
                .startDate(LocalDateTime.of(2026, 1, 1, 0, 0))
                .endDate(LocalDateTime.of(2026, 12, 31, 23, 59))
                .build();

        when(bannerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(bannerRepository.save(any(Banner.class))).thenReturn(existing);

        bannerService.updateBanner(1L, updatedFields, admin);

        assertEquals("New Title", existing.getTitle());
        assertEquals("New Sub", existing.getSubtitle());
        assertEquals("#fff", existing.getColor());
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), existing.getStartDate());
        assertEquals(LocalDateTime.of(2026, 12, 31, 23, 59), existing.getEndDate());
    }

    @Test
    void updateBanner_shouldToggleActive() {
        Banner existing = Banner.builder()
                .id(1L).title("Banner").active(true).build();
        Banner updatedFields = Banner.builder().active(false).build();

        when(bannerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(bannerRepository.save(any(Banner.class))).thenReturn(existing);

        bannerService.updateBanner(1L, updatedFields, admin);

        assertFalse(existing.getActive());
    }

    @Test
    void updateBanner_shouldThrowWhenNotFound() {
        Banner updatedFields = Banner.builder().title("New").build();
        when(bannerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> bannerService.updateBanner(99L, updatedFields, admin));
    }

    // ─── createBanner ────────────────────────────────────────────

    @Test
    void createBanner_shouldSetCreatedByAndAudit() {
        Banner banner = Banner.builder().id(1L).title("New Banner").build();
        when(bannerRepository.save(any(Banner.class))).thenReturn(banner);

        Banner result = bannerService.createBanner(banner, admin);

        assertEquals(admin, banner.getCreatedBy());
        verify(auditService).audit(admin, "CREATE_BANNER", "BANNER", 1L, "New Banner");
    }

    // ─── deleteBanner ────────────────────────────────────────────

    @Test
    void deleteBanner_shouldDeleteAndAudit() {
        bannerService.deleteBanner(1L, admin);

        verify(bannerRepository).deleteById(1L);
        verify(auditService).audit(admin, "DELETE_BANNER", "BANNER", 1L, null);
    }

    // ─── isWithinDateRange helper ────────────────────────────────

    @Test
    void isWithinDateRange_shouldReturnTrueForNullDates() {
        Banner banner = Banner.builder().startDate(null).endDate(null).build();
        assertTrue(bannerService.isWithinDateRange(banner, LocalDateTime.now()));
    }

    @Test
    void isWithinDateRange_shouldReturnTrueWhenNowBetweenStartAndEnd() {
        LocalDateTime now = LocalDateTime.now();
        Banner banner = Banner.builder()
                .startDate(now.minusHours(1))
                .endDate(now.plusHours(1)).build();
        assertTrue(bannerService.isWithinDateRange(banner, now));
    }

    @Test
    void isWithinDateRange_shouldReturnFalseWhenNowBeforeStart() {
        LocalDateTime now = LocalDateTime.now();
        Banner banner = Banner.builder()
                .startDate(now.plusHours(1))
                .endDate(now.plusHours(2)).build();
        assertFalse(bannerService.isWithinDateRange(banner, now));
    }

    @Test
    void isWithinDateRange_shouldReturnFalseWhenNowAfterEnd() {
        LocalDateTime now = LocalDateTime.now();
        Banner banner = Banner.builder()
                .startDate(now.minusHours(2))
                .endDate(now.minusHours(1)).build();
        assertFalse(bannerService.isWithinDateRange(banner, now));
    }
}
