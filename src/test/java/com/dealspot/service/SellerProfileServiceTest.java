package com.dealspot.service;

import com.dealspot.dto.SellerProfileResponse;
import com.dealspot.entity.Listing;
import com.dealspot.entity.Review;
import com.dealspot.entity.User;
import com.dealspot.repository.ListingRepository;
import com.dealspot.repository.ReviewRepository;
import com.dealspot.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SellerProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private SellerProfileService sellerProfileService;

    private User seller;
    private User buyer;

    @BeforeEach
    void setUp() {
        seller = User.builder()
                .id(1L)
                .phone("9876543210")
                .name("Test Seller")
                .district("Mysuru")
                .role("USER")
                .banned(false)
                .createdAt(LocalDateTime.of(2024, 1, 15, 10, 0))
                .build();

        buyer = User.builder()
                .id(2L)
                .phone("9876543211")
                .name("Test Buyer")
                .role("USER")
                .banned(false)
                .build();
    }

    @Test
    void getSellerProfile_shouldReturnCompleteProfile() {
        Listing listing1 = Listing.builder()
                .id(10L)
                .title("Tractor for rent")
                .category("tractor-rental")
                .price(5000.0)
                .status("ACTIVE")
                .user(seller)
                .images(Collections.emptyList())
                .build();

        Listing listing2 = Listing.builder()
                .id(11L)
                .title("Farm equipment")
                .category("farm-equipment")
                .price(15000.0)
                .status("ACTIVE")
                .user(seller)
                .images(Collections.emptyList())
                .build();

        List<Listing> activeListings = Arrays.asList(listing1, listing2);
        Page<Listing> listingsPage = new PageImpl<>(activeListings);

        Review review = Review.builder()
                .id(1L)
                .buyer(buyer)
                .seller(seller)
                .rating((short) 4)
                .comment("Good seller")
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(listingRepository.findByUserIdAndStatus(eq(1L), eq("ACTIVE"), any(Pageable.class)))
                .thenReturn(listingsPage);
        when(reviewRepository.averageRatingBySellerId(1L)).thenReturn(4.0);
        when(reviewRepository.findBySellerId(1L)).thenReturn(List.of(review));

        SellerProfileResponse profile = sellerProfileService.getSellerProfile(1L);

        assertEquals(1L, profile.getId());
        assertEquals("Test Seller", profile.getName());
        assertEquals("Mysuru", profile.getDistrict());
        assertEquals(LocalDateTime.of(2024, 1, 15, 10, 0), profile.getMemberSince());
        assertEquals(2, profile.getTotalListings());
        assertEquals(4.0, profile.getAverageRating());
        assertEquals(1, profile.getTotalReviews());
        assertEquals(2, profile.getListings().size());
        assertEquals(1, profile.getReviews().size());
    }

    @Test
    void getSellerProfile_shouldHandleSellerWithNoListingsOrReviews() {
        Page<Listing> emptyPage = new PageImpl<>(Collections.emptyList());

        when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(listingRepository.findByUserIdAndStatus(eq(1L), eq("ACTIVE"), any(Pageable.class)))
                .thenReturn(emptyPage);
        when(reviewRepository.averageRatingBySellerId(1L)).thenReturn(0.0);
        when(reviewRepository.findBySellerId(1L)).thenReturn(Collections.emptyList());

        SellerProfileResponse profile = sellerProfileService.getSellerProfile(1L);

        assertEquals(0, profile.getTotalListings());
        assertEquals(0.0, profile.getAverageRating());
        assertEquals(0, profile.getTotalReviews());
        assertTrue(profile.getListings().isEmpty());
        assertTrue(profile.getReviews().isEmpty());
    }

    @Test
    void getSellerProfile_shouldRoundAverageRatingToOneDecimal() {
        Page<Listing> emptyPage = new PageImpl<>(Collections.emptyList());

        when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(listingRepository.findByUserIdAndStatus(eq(1L), eq("ACTIVE"), any(Pageable.class)))
                .thenReturn(emptyPage);
        when(reviewRepository.averageRatingBySellerId(1L)).thenReturn(4.3333);
        when(reviewRepository.findBySellerId(1L)).thenReturn(Collections.emptyList());

        SellerProfileResponse profile = sellerProfileService.getSellerProfile(1L);

        assertEquals(4.3, profile.getAverageRating());
    }

    @Test
    void getSellerProfile_shouldThrowIfSellerNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> sellerProfileService.getSellerProfile(99L));
        assertTrue(ex.getMessage().contains("Seller not found"));
    }
}
