package com.dealspot.controller;

import com.dealspot.dto.ConversationResponse;
import com.dealspot.dto.MessageResponse;
import com.dealspot.dto.SendMessageRequest;
import com.dealspot.dto.StartConversationRequest;
import com.dealspot.entity.ChatConversation;
import com.dealspot.entity.ChatMessage;
import com.dealspot.entity.MessageType;
import com.dealspot.entity.User;
import com.dealspot.repository.ChatMessageRepository;
import com.dealspot.service.ChatService;
import com.dealspot.service.CloudinaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final CloudinaryService cloudinaryService;
    private final ChatMessageRepository chatMessageRepository;

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationResponse>> getConversations(
            @AuthenticationPrincipal User user) {
        List<ChatConversation> conversations = chatService.getConversations(user.getId());
        List<ConversationResponse> responses = conversations.stream()
                .map(conv -> {
                    long unreadCount = chatMessageRepository
                            .countByConversationIdAndReadFalseAndSenderIdNot(conv.getId(), user.getId());
                    return ConversationResponse.fromChatConversation(conv, unreadCount);
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/conversations/{id}")
    public ResponseEntity<Map<String, Object>> getConversation(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        List<ChatMessage> messages = chatService.getMessages(id, user.getId());
        List<MessageResponse> messageResponses = messages.stream()
                .map(MessageResponse::fromChatMessage)
                .collect(Collectors.toList());

        // Also return conversation metadata
        List<ChatConversation> conversations = chatService.getConversations(user.getId());
        ChatConversation conversation = conversations.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        ConversationResponse conversationResponse = ConversationResponse.fromChatConversation(conversation);

        return ResponseEntity.ok(Map.of(
                "conversation", conversationResponse,
                "messages", messageResponses
        ));
    }

    @PostMapping("/conversations")
    public ResponseEntity<ConversationResponse> startConversation(
            @Valid @RequestBody StartConversationRequest request,
            @AuthenticationPrincipal User user) {
        ChatConversation conversation = chatService.getOrCreateConversation(
                user.getId(), request.getListingId(), request.getMessage());
        ConversationResponse response = ConversationResponse.fromChatConversation(conversation);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal User user) {
        ChatMessage message = chatService.sendMessage(id, user.getId(), request.getContent(), MessageType.TEXT);
        MessageResponse response = MessageResponse.fromChatMessage(message);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/conversations/{id}/voice")
    public ResponseEntity<MessageResponse> uploadVoiceMessage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) {
        // Validate file size (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
        }

        // Upload voice file to Cloudinary
        String voiceUrl = cloudinaryService.uploadImageUrl(file);

        // Send voice message
        ChatMessage message = chatService.sendVoiceMessage(id, user.getId(), voiceUrl);
        MessageResponse response = MessageResponse.fromChatMessage(message);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal User user) {
        long count = chatService.getUnreadCount(user.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }
}
