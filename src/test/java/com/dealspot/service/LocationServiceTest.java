package com.dealspot.service;

import com.dealspot.entity.User;
import com.dealspot.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LocationService locationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .phone("9876543210")
                .name("Test User")
                .role("USER")
                .banned(false)
                .preferredDistrict(null)
                .build();
    }

    @Test
    void getDistricts_shouldReturn31Districts() {
        List<String[]> districts = locationService.getDistricts();
        assertEquals(31, districts.size());
    }

    @Test
    void getDistricts_shouldContainBothEnglishAndKannadaNames() {
        List<String[]> districts = locationService.getDistricts();
        for (String[] district : districts) {
            assertEquals(2, district.length);
            assertNotNull(district[0]); // English name
            assertNotNull(district[1]); // Kannada name
            assertFalse(district[0].isEmpty());
            assertFalse(district[1].isEmpty());
        }
    }

    @Test
    void getDistricts_shouldContainBengaluruUrban() {
        List<String[]> districts = locationService.getDistricts();
        boolean found = districts.stream()
                .anyMatch(d -> "Bengaluru Urban".equals(d[0]));
        assertTrue(found);
    }

    @Test
    void getDistricts_shouldContainVijayanagara() {
        List<String[]> districts = locationService.getDistricts();
        boolean found = districts.stream()
                .anyMatch(d -> "Vijayanagara".equals(d[0]));
        assertTrue(found);
    }

    @Test
    void setUserDistrict_shouldUpdateUserPreferredDistrict() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        locationService.setUserDistrict(1L, "Mysuru");

        assertEquals("Mysuru", testUser.getPreferredDistrict());
        verify(userRepository).save(testUser);
    }

    @Test
    void setUserDistrict_shouldThrowIfUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> locationService.setUserDistrict(99L, "Mysuru"));
        assertTrue(ex.getMessage().contains("User not found"));
    }

    @Test
    void getUserDistrict_shouldReturnPreferredDistrict() {
        testUser.setPreferredDistrict("Hassan");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        String district = locationService.getUserDistrict(1L);

        assertEquals("Hassan", district);
    }

    @Test
    void getUserDistrict_shouldReturnNullIfNotSet() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        String district = locationService.getUserDistrict(1L);

        assertNull(district);
    }

    @Test
    void getUserDistrict_shouldThrowIfUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> locationService.getUserDistrict(99L));
        assertTrue(ex.getMessage().contains("User not found"));
    }
}
