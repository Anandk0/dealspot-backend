package com.dealspot.service;

import com.dealspot.entity.*;
import com.dealspot.repository.*;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based test for ChatService conversation uniqueness.
 *
 * Validates: Requirements 3.1, 3.4, 3.6
 */
@Tag("buyer-experience")
@Tag("chat-service")
class ChatServicePropertyTest {

    private ChatConversationRepository chatConversationRepository;
    private ChatMessageRepository chatMessageRepository;
    private ListingRepository listingRepository;
    private UserRepository userRepository;
    private NotificationService notificationService;
    private ChatService chatService;

    @BeforeTry
    void setUp() {
        this.chatConversationRepository = mock(ChatConversationRepository.class);
        this.chatMessageRepository = mock(ChatMessageRepository.class);
        this.listingRepository = mock(ListingRepository.class);
        this.userRepository = mock(UserRepository.class);
        this.notificationService = mock(NotificationService.class);
        this.chatService = new ChatService(
                chatConversationRepository,
                chatMessageRepository,
                listingRepository,
                userRepository,
                notificationService
        );
    }

    /**
     * Property 6: Conversation uniqueness per buyer-seller-listing.
     *
     * For any valid (buyerId, listingId) pair, calling getOrCreateConversation
     * multiple times always returns the same conversation ID — the method is
     * idempotent with respect to the buyer-listing pair.
     *
     * Validates: Requirements 3.1, 3.4, 3.6
     */
    @Property(tries = 100)
    void getOrCreateConversation_isIdempotent_forSameBuyerListingPair(
            @ForAll("buyerIds") Long buyerId,
            @ForAll("listingIds") Long listingId,
            @ForAll("callCounts") int callCount
    ) {
        // Setup: distinct buyer and seller
        Long sellerId = buyerId + 1000L;

        User buyer = User.builder().id(buyerId).name("Buyer-" + buyerId).phone("900000" + buyerId).password("pass").build();
        User seller = User.builder().id(sellerId).name("Seller-" + sellerId).phone("800000" + sellerId).password("pass").build();
        Listing listing = Listing.builder().id(listingId).title("Listing-" + listingId).category("livestock").user(seller).build();

        ChatConversation existingConversation = ChatConversation.builder()
                .id(buyerId * 100 + listingId)
                .listing(listing)
                .buyer(buyer)
                .seller(seller)
                .build();

        // Simulate: conversation already exists for this buyer-listing pair
        when(chatConversationRepository.findByListingIdAndBuyerId(listingId, buyerId))
                .thenReturn(Optional.of(existingConversation));

        // Call getOrCreateConversation multiple times
        ChatConversation firstResult = chatService.getOrCreateConversation(buyerId, listingId, "Hello");

        for (int i = 1; i < callCount; i++) {
            ChatConversation subsequentResult = chatService.getOrCreateConversation(buyerId, listingId, "Message " + i);
            // Assert: same conversation is returned every time
            assertEquals(firstResult.getId(), subsequentResult.getId(),
                    "getOrCreateConversation must return the same conversation for buyer=" + buyerId + " listing=" + listingId);
        }

        // Verify: repository's save was never called since conversation already exists
        verify(chatConversationRepository, never()).save(any(ChatConversation.class));
    }

    /**
     * Property 6 (creation path): When no conversation exists for the buyer-listing pair,
     * calling getOrCreateConversation creates exactly one conversation and subsequent calls
     * return that same conversation.
     *
     * Validates: Requirements 3.1, 3.4, 3.6
     */
    @Property(tries = 100)
    void getOrCreateConversation_createsOnce_thenReturnsExisting(
            @ForAll("buyerIds") Long buyerId,
            @ForAll("listingIds") Long listingId
    ) {
        // Setup: distinct buyer and seller
        Long sellerId = buyerId + 1000L;

        User buyer = User.builder().id(buyerId).name("Buyer-" + buyerId).phone("900000" + buyerId).password("pass").build();
        User seller = User.builder().id(sellerId).name("Seller-" + sellerId).phone("800000" + sellerId).password("pass").build();
        Listing listing = Listing.builder().id(listingId).title("Listing-" + listingId).category("livestock").user(seller).build();

        Long conversationId = buyerId * 100 + listingId;
        ChatConversation createdConversation = ChatConversation.builder()
                .id(conversationId)
                .listing(listing)
                .buyer(buyer)
                .seller(seller)
                .build();

        // First call: no existing conversation → create
        // Second+ calls: existing conversation found
        when(chatConversationRepository.findByListingIdAndBuyerId(listingId, buyerId))
                .thenReturn(Optional.empty())     // first call
                .thenReturn(Optional.of(createdConversation)); // subsequent calls

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(userRepository.findById(buyerId)).thenReturn(Optional.of(buyer));
        when(chatConversationRepository.save(any(ChatConversation.class))).thenReturn(createdConversation);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(ChatMessage.builder().id(1L).build());

        // First call creates the conversation
        ChatConversation firstResult = chatService.getOrCreateConversation(buyerId, listingId, "Hello");
        assertNotNull(firstResult, "First call should create a conversation");
        assertEquals(conversationId, firstResult.getId());

        // Second call finds the existing conversation
        ChatConversation secondResult = chatService.getOrCreateConversation(buyerId, listingId, "Again");
        assertEquals(firstResult.getId(), secondResult.getId(),
                "Second call must return the same conversation ID");

        // Verify: save was called exactly once (for the creation)
        verify(chatConversationRepository, times(1)).save(any(ChatConversation.class));
    }

    // ─── Providers ────────────────────────────────────────

    @Provide
    Arbitrary<Long> buyerIds() {
        return Arbitraries.longs().between(1L, 500L);
    }

    @Provide
    Arbitrary<Long> listingIds() {
        return Arbitraries.longs().between(1L, 200L);
    }

    @Provide
    Arbitrary<Integer> callCounts() {
        return Arbitraries.integers().between(2, 10);
    }
}
