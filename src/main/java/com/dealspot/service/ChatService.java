package com.dealspot.service;

import com.dealspot.entity.*;
import com.dealspot.repository.*;
import com.dealspot.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ListingRepository listingRepository;
    private final NotificationService notificationService;

    public Conversation getOrCreateConversation(Long listingId, User buyer) {
        return conversationRepository.findByListingIdAndBuyerId(listingId, buyer.getId())
                .orElseGet(() -> {
                    Listing listing = listingRepository.findById(listingId)
                            .orElseThrow(() -> new RuntimeException("Listing not found"));

                    if (listing.getUser().getId().equals(buyer.getId())) {
                        throw new RuntimeException("Cannot start conversation with yourself");
                    }

                    Conversation conv = Conversation.builder()
                            .listing(listing)
                            .buyer(buyer)
                            .seller(listing.getUser())
                            .build();
                    return conversationRepository.save(conv);
                });
    }

    public Page<Conversation> getMyConversations(Long userId, int page, int size) {
        return conversationRepository.findByUserId(userId, PaginationUtil.createPageable(page, size));
    }

    @Transactional
    public Message sendMessage(Long conversationId, String content, User sender) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        // Verify sender is part of this conversation
        if (!conv.getBuyer().getId().equals(sender.getId()) &&
            !conv.getSeller().getId().equals(sender.getId())) {
            throw new RuntimeException("Not authorized to send message in this conversation");
        }

        Message message = Message.builder()
                .conversation(conv)
                .sender(sender)
                .content(content)
                .build();
        message = messageRepository.save(message);

        // Update last message time
        conv.setLastMessageAt(LocalDateTime.now());
        conversationRepository.save(conv);

        // Notify the other user
        User recipient = conv.getBuyer().getId().equals(sender.getId()) ? conv.getSeller() : conv.getBuyer();
        notificationService.create(recipient, "ಹೊಸ ಸಂದೇಶ", sender.getName() + ": " + content.substring(0, Math.min(50, content.length())), "CHAT", "CONVERSATION", conversationId);

        return message;
    }

    public Page<Message> getMessages(Long conversationId, User user, int page, int size) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        if (!conv.getBuyer().getId().equals(user.getId()) &&
            !conv.getSeller().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized");
        }

        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId, PaginationUtil.createPageable(page, size));
    }

    @Transactional
    public void markAsRead(Long conversationId, User user) {
        messageRepository.markAsRead(conversationId, user.getId());
    }
}
