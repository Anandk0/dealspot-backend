package com.dealspot.service;

import com.dealspot.entity.*;
import com.dealspot.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatConversationRepository chatConversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Finds an existing conversation for the buyer-listing pair, or creates a new one
     * and sends the initial message. Enforces uniqueness per buyer-listing pair.
     */
    @Transactional
    public ChatConversation getOrCreateConversation(Long buyerId, Long listingId, String initialMessage) {
        // Check if conversation already exists for this buyer-listing pair
        return chatConversationRepository.findByListingIdAndBuyerId(listingId, buyerId)
                .orElseGet(() -> {
                    Listing listing = listingRepository.findById(listingId)
                            .orElseThrow(() -> new RuntimeException("Listing not found"));

                    User buyer = userRepository.findById(buyerId)
                            .orElseThrow(() -> new RuntimeException("Buyer not found"));

                    if (listing.getUser().getId().equals(buyerId)) {
                        throw new RuntimeException("Cannot start conversation with yourself");
                    }

                    User seller = listing.getUser();

                    ChatConversation conversation = ChatConversation.builder()
                            .listing(listing)
                            .buyer(buyer)
                            .seller(seller)
                            .lastMessageAt(LocalDateTime.now())
                            .build();
                    conversation = chatConversationRepository.save(conversation);

                    // Send the initial message if provided
                    if (initialMessage != null && !initialMessage.isBlank()) {
                        ChatMessage message = ChatMessage.builder()
                                .conversation(conversation)
                                .sender(buyer)
                                .content(initialMessage)
                                .messageType(MessageType.TEXT)
                                .build();
                        chatMessageRepository.save(message);

                        // Notify the seller about the new conversation
                        notificationService.create(
                                seller,
                                "ಹೊಸ ಸಂದೇಶ",
                                buyer.getName() + ": " + initialMessage.substring(0, Math.min(50, initialMessage.length())),
                                "CHAT",
                                "CONVERSATION",
                                conversation.getId()
                        );
                    }

                    return conversation;
                });
    }

    /**
     * Sends a message in an existing conversation. Updates lastMessageAt on the conversation.
     */
    @Transactional
    public ChatMessage sendMessage(Long conversationId, Long senderId, String content, MessageType messageType) {
        ChatConversation conversation = chatConversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify sender is part of this conversation
        if (!conversation.getBuyer().getId().equals(senderId) &&
            !conversation.getSeller().getId().equals(senderId)) {
            throw new RuntimeException("Not authorized to send message in this conversation");
        }

        ChatMessage message = ChatMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .content(content)
                .messageType(messageType)
                .build();
        message = chatMessageRepository.save(message);

        // Update last message time on conversation
        conversation.setLastMessageAt(LocalDateTime.now());
        chatConversationRepository.save(conversation);

        // Notify the other user
        User recipient = conversation.getBuyer().getId().equals(senderId)
                ? conversation.getSeller()
                : conversation.getBuyer();

        String notificationMessage = messageType == MessageType.VOICE
                ? sender.getName() + ": 🎤 ಧ್ವನಿ ಸಂದೇಶ"
                : sender.getName() + ": " + content.substring(0, Math.min(50, content.length()));

        notificationService.create(
                recipient,
                "ಹೊಸ ಸಂದೇಶ",
                notificationMessage,
                "CHAT",
                "CONVERSATION",
                conversationId
        );

        return message;
    }

    /**
     * Returns all conversations where the user is either the buyer or the seller,
     * ordered by lastMessageAt descending.
     */
    public List<ChatConversation> getConversations(Long userId) {
        return chatConversationRepository.findByBuyerIdOrSellerId(userId);
    }

    /**
     * Returns all messages in a conversation and marks unread messages (sent by others) as read.
     */
    @Transactional
    public List<ChatMessage> getMessages(Long conversationId, Long userId) {
        ChatConversation conversation = chatConversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        // Verify user is part of this conversation
        if (!conversation.getBuyer().getId().equals(userId) &&
            !conversation.getSeller().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to view this conversation");
        }

        List<ChatMessage> messages = chatMessageRepository.findByConversationIdOrderByCreatedAt(conversationId);

        // Mark messages as read where sender is not the current user
        messages.stream()
                .filter(m -> !m.getSender().getId().equals(userId) && !m.getRead())
                .forEach(m -> {
                    m.setRead(true);
                    chatMessageRepository.save(m);
                });

        return messages;
    }

    /**
     * Sends a voice message in an existing conversation.
     */
    @Transactional
    public ChatMessage sendVoiceMessage(Long conversationId, Long senderId, String voiceUrl) {
        ChatConversation conversation = chatConversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify sender is part of this conversation
        if (!conversation.getBuyer().getId().equals(senderId) &&
            !conversation.getSeller().getId().equals(senderId)) {
            throw new RuntimeException("Not authorized to send message in this conversation");
        }

        ChatMessage message = ChatMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .voiceUrl(voiceUrl)
                .messageType(MessageType.VOICE)
                .build();
        message = chatMessageRepository.save(message);

        // Update last message time on conversation
        conversation.setLastMessageAt(LocalDateTime.now());
        chatConversationRepository.save(conversation);

        // Notify the other user
        User recipient = conversation.getBuyer().getId().equals(senderId)
                ? conversation.getSeller()
                : conversation.getBuyer();

        notificationService.create(
                recipient,
                "ಹೊಸ ಸಂದೇಶ",
                sender.getName() + ": 🎤 ಧ್ವನಿ ಸಂದೇಶ",
                "CHAT",
                "CONVERSATION",
                conversationId
        );

        return message;
    }

    /**
     * Returns the total unread message count for a user across all their conversations.
     */
    public long getUnreadCount(Long userId) {
        List<ChatConversation> conversations = chatConversationRepository.findByBuyerIdOrSellerId(userId);
        return conversations.stream()
                .mapToLong(conv -> chatMessageRepository.countByConversationIdAndReadFalseAndSenderIdNot(conv.getId(), userId))
                .sum();
    }
}
