package com.dealspot.service;

import com.dealspot.entity.*;
import com.dealspot.repository.*;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Property-based test for the one-active-offer-per-buyer-per-listing constraint.
 *
 * Property 8: One active offer per buyer per listing
 * For any buyer and listing combination, there SHALL be at most one offer with status PENDING
 * at any time. Submitting a new offer when one is already PENDING SHALL be rejected.
 *
 * Validates: Requirements 4.6
 */
@Tag("buyer-experience")
@Tag("one-active-offer")
class OfferOneActivePropertyTest {

    private final OfferRepository offerRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final OfferService offerService;

    OfferOneActivePropertyTest() {
        this.offerRepository = mock(OfferRepository.class);
        this.listingRepository = mock(ListingRepository.class);
        this.userRepository = mock(UserRepository.class);
        this.notificationService = mock(NotificationService.class);
        this.offerService = new OfferService(offerRepository, listingRepository, userRepository, notificationService);
    }

    /**
     * Property 8: One active offer per buyer per listing.
     *
     * For any random (buyerId, listingId) pair where a PENDING offer already exists,
     * attempting to create a second offer MUST throw IllegalStateException.
     *
     * Validates: Requirements 4.6
     */
    @Property(tries = 100)
    void secondOfferOnSameListingIsRejectedWhenPendingExists(
            @ForAll("buyerIds") Long buyerId,
            @ForAll("listingIds") Long listingId,
            @ForAll("offerAmounts") BigDecimal newOfferAmount
    ) {
        // Arrange: Set up a seller who owns the listing (different from buyer)
        Long sellerId = buyerId + 1000L; // Ensure seller != buyer

        User buyer = User.builder().id(buyerId).name("Buyer").phone("9000000000").password("pass").build();
        User seller = User.builder().id(sellerId).name("Seller").phone("9000000001").password("pass").build();
        Listing listing = Listing.builder()
                .id(listingId)
                .title("Test Listing")
                .category("livestock")
                .price(10000.0)
                .user(seller)
                .build();

        // Simulate an existing PENDING offer for this buyer-listing pair
        Offer existingOffer = Offer.builder()
                .id(999L)
                .buyer(buyer)
                .seller(seller)
                .listing(listing)
                .amount(BigDecimal.valueOf(8000))
                .status(OfferStatus.PENDING)
                .build();

        // Mock repository behavior
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(userRepository.findById(buyerId)).thenReturn(Optional.of(buyer));
        when(offerRepository.findByBuyerIdAndListingIdAndStatus(buyerId, listingId, OfferStatus.PENDING))
                .thenReturn(Optional.of(existingOffer));

        // Act & Assert: Creating a second offer should throw IllegalStateException
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> offerService.createOffer(buyerId, listingId, newOfferAmount, "New offer message"),
                "Expected IllegalStateException when a PENDING offer already exists for buyer "
                        + buyerId + " on listing " + listingId
        );

        assertTrue(ex.getMessage().contains("Active offer already exists"),
                "Exception message should indicate an active offer already exists, got: " + ex.getMessage());

        // Verify that no new offer was saved
        verify(offerRepository, never()).save(any(Offer.class));
    }

    // ─── Providers ────────────────────────────────────────

    @Provide
    Arbitrary<Long> buyerIds() {
        return Arbitraries.longs().between(1L, 10000L);
    }

    @Provide
    Arbitrary<Long> listingIds() {
        return Arbitraries.longs().between(1L, 10000L);
    }

    @Provide
    Arbitrary<BigDecimal> offerAmounts() {
        // Generate valid offer amounts between 1 and 20000 (within 2x of listing price 10000)
        return Arbitraries.bigDecimals()
                .between(BigDecimal.ONE, BigDecimal.valueOf(20000))
                .ofScale(2);
    }
}
