package com.dealspot.service;

import com.dealspot.entity.*;
import com.dealspot.repository.*;
import net.jqwik.api.*;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based test for OfferService state machine transitions.
 *
 * Validates: Requirements 4.4, 4.5
 */
@Tag("buyer-experience")
@Tag("offer-state-machine")
class OfferServicePropertyTest {

    private final OfferRepository offerRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final OfferService offerService;

    OfferServicePropertyTest() {
        this.offerRepository = mock(OfferRepository.class);
        this.listingRepository = mock(ListingRepository.class);
        this.userRepository = mock(UserRepository.class);
        this.notificationService = mock(NotificationService.class);
        this.offerService = new OfferService(
                offerRepository,
                listingRepository,
                userRepository,
                notificationService
        );
    }

    /**
     * Property 7: Offer state machine transitions — acceptOffer on PENDING → ACCEPTED.
     *
     * For any offer in PENDING state, calling acceptOffer with the correct seller ID
     * shall always transition the offer status to ACCEPTED.
     *
     * Validates: Requirements 4.4, 4.5
     */
    @Property(tries = 100)
    void acceptOffer_onPending_alwaysTransitionsToAccepted(
            @ForAll("offerIds") Long offerId,
            @ForAll("sellerIds") Long sellerId,
            @ForAll("amounts") BigDecimal amount
    ) {
        // Setup: create a PENDING offer
        User buyer = User.builder().id(sellerId + 1000L).name("Buyer").phone("9000001").password("pass").build();
        User seller = User.builder().id(sellerId).name("Seller").phone("8000001").password("pass").build();
        Listing listing = Listing.builder().id(1L).title("Test Listing").category("livestock").user(seller).price(10000.0).build();

        Offer pendingOffer = Offer.builder()
                .id(offerId)
                .listing(listing)
                .buyer(buyer)
                .seller(seller)
                .amount(amount)
                .status(OfferStatus.PENDING)
                .build();

        when(offerRepository.findById(offerId)).thenReturn(Optional.of(pendingOffer));
        when(offerRepository.save(any(Offer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Offer result = offerService.acceptOffer(offerId, sellerId);

        // Assert: status is always ACCEPTED
        assertEquals(OfferStatus.ACCEPTED, result.getStatus(),
                "acceptOffer on PENDING offer must always result in ACCEPTED status");
        assertNotNull(result.getUpdatedAt(),
                "updatedAt must be set after state transition");
    }

    /**
     * Property 7: Offer state machine transitions — rejectOffer with null counterAmount on PENDING → REJECTED.
     *
     * For any offer in PENDING state, calling rejectOffer with null counterAmount
     * shall always transition the offer status to REJECTED.
     *
     * Validates: Requirements 4.4, 4.5
     */
    @Property(tries = 100)
    void rejectOffer_withNullCounter_onPending_alwaysTransitionsToRejected(
            @ForAll("offerIds") Long offerId,
            @ForAll("sellerIds") Long sellerId,
            @ForAll("amounts") BigDecimal amount
    ) {
        // Setup: create a PENDING offer
        User buyer = User.builder().id(sellerId + 1000L).name("Buyer").phone("9000001").password("pass").build();
        User seller = User.builder().id(sellerId).name("Seller").phone("8000001").password("pass").build();
        Listing listing = Listing.builder().id(1L).title("Test Listing").category("livestock").user(seller).price(10000.0).build();

        Offer pendingOffer = Offer.builder()
                .id(offerId)
                .listing(listing)
                .buyer(buyer)
                .seller(seller)
                .amount(amount)
                .status(OfferStatus.PENDING)
                .build();

        when(offerRepository.findById(offerId)).thenReturn(Optional.of(pendingOffer));
        when(offerRepository.save(any(Offer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act: reject with null counter amount
        Offer result = offerService.rejectOffer(offerId, sellerId, null);

        // Assert: status is always REJECTED
        assertEquals(OfferStatus.REJECTED, result.getStatus(),
                "rejectOffer with null counterAmount on PENDING offer must always result in REJECTED status");
        assertNull(result.getCounterAmount(),
                "counterAmount must remain null for a plain rejection");
    }

    /**
     * Property 7: Offer state machine transitions — rejectOffer with non-null counterAmount on PENDING → COUNTER.
     *
     * For any offer in PENDING state, calling rejectOffer with a positive counterAmount
     * shall always transition the offer status to COUNTER.
     *
     * Validates: Requirements 4.4, 4.5
     */
    @Property(tries = 100)
    void rejectOffer_withCounterAmount_onPending_alwaysTransitionsToCounter(
            @ForAll("offerIds") Long offerId,
            @ForAll("sellerIds") Long sellerId,
            @ForAll("amounts") BigDecimal offerAmount,
            @ForAll("counterAmounts") BigDecimal counterAmount
    ) {
        // Setup: create a PENDING offer
        User buyer = User.builder().id(sellerId + 1000L).name("Buyer").phone("9000001").password("pass").build();
        User seller = User.builder().id(sellerId).name("Seller").phone("8000001").password("pass").build();
        Listing listing = Listing.builder().id(1L).title("Test Listing").category("livestock").user(seller).price(10000.0).build();

        Offer pendingOffer = Offer.builder()
                .id(offerId)
                .listing(listing)
                .buyer(buyer)
                .seller(seller)
                .amount(offerAmount)
                .status(OfferStatus.PENDING)
                .build();

        when(offerRepository.findById(offerId)).thenReturn(Optional.of(pendingOffer));
        when(offerRepository.save(any(Offer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act: reject with a positive counter amount
        Offer result = offerService.rejectOffer(offerId, sellerId, counterAmount);

        // Assert: status is always COUNTER
        assertEquals(OfferStatus.COUNTER, result.getStatus(),
                "rejectOffer with positive counterAmount on PENDING offer must always result in COUNTER status");
        assertEquals(counterAmount, result.getCounterAmount(),
                "counterAmount must be set to the provided value");
    }

    /**
     * Property 7: Offer state machine transitions — any transition on non-PENDING → IllegalStateException.
     *
     * For any offer NOT in PENDING state, calling acceptOffer or rejectOffer
     * shall always throw IllegalStateException.
     *
     * Validates: Requirements 4.4, 4.5
     */
    @Property(tries = 100)
    void anyTransition_onNonPending_alwaysThrowsIllegalStateException(
            @ForAll("offerIds") Long offerId,
            @ForAll("sellerIds") Long sellerId,
            @ForAll("nonPendingStatuses") OfferStatus nonPendingStatus,
            @ForAll("actions") String action
    ) {
        // Setup: create an offer in a non-PENDING state
        User buyer = User.builder().id(sellerId + 1000L).name("Buyer").phone("9000001").password("pass").build();
        User seller = User.builder().id(sellerId).name("Seller").phone("8000001").password("pass").build();
        Listing listing = Listing.builder().id(1L).title("Test Listing").category("livestock").user(seller).price(10000.0).build();

        Offer nonPendingOffer = Offer.builder()
                .id(offerId)
                .listing(listing)
                .buyer(buyer)
                .seller(seller)
                .amount(BigDecimal.valueOf(5000))
                .status(nonPendingStatus)
                .build();

        when(offerRepository.findById(offerId)).thenReturn(Optional.of(nonPendingOffer));

        // Act & Assert: any state transition attempt throws IllegalStateException
        if ("accept".equals(action)) {
            assertThrows(IllegalStateException.class,
                    () -> offerService.acceptOffer(offerId, sellerId),
                    "acceptOffer on " + nonPendingStatus + " offer must throw IllegalStateException");
        } else {
            assertThrows(IllegalStateException.class,
                    () -> offerService.rejectOffer(offerId, sellerId, BigDecimal.valueOf(6000)),
                    "rejectOffer on " + nonPendingStatus + " offer must throw IllegalStateException");
        }

        // Verify: no save was called (state was not mutated)
        verify(offerRepository, never()).save(any(Offer.class));
    }

    // ─── Providers ────────────────────────────────────────

    @Provide
    Arbitrary<Long> offerIds() {
        return Arbitraries.longs().between(1L, 1000L);
    }

    @Provide
    Arbitrary<Long> sellerIds() {
        return Arbitraries.longs().between(1L, 500L);
    }

    @Provide
    Arbitrary<BigDecimal> amounts() {
        return Arbitraries.bigDecimals()
                .between(BigDecimal.valueOf(100), BigDecimal.valueOf(50000))
                .ofScale(2);
    }

    @Provide
    Arbitrary<BigDecimal> counterAmounts() {
        return Arbitraries.bigDecimals()
                .between(BigDecimal.valueOf(100), BigDecimal.valueOf(100000))
                .ofScale(2);
    }

    @Provide
    Arbitrary<OfferStatus> nonPendingStatuses() {
        return Arbitraries.of(OfferStatus.ACCEPTED, OfferStatus.REJECTED, OfferStatus.COUNTER);
    }

    @Provide
    Arbitrary<String> actions() {
        return Arbitraries.of("accept", "reject");
    }
}
