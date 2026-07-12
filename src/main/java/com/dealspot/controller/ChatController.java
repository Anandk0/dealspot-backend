package com.dealspot.controller;

import com.dealspot.dto.ConversationResponse;
import com.dealspot.dto.MessageResponse;
import com.dealspot.dto.SendMessageRequest;
import com.dealspot.entity.User;
import com.dealspot.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/start/{listingId}")
    public ResponseEntity<ConversationResponse> startConversation(
            @PathVariable Long listingId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ConversationResponse.fromEntity(
                chatService.getOrCreateConversation(listingId, user)));
    }

    @GetMapping("/conversations")
    public ResponseEntity<Page<ConversationResponse>> getConversations(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(chatService.getMyConversations(user.getId(), page, size)
                .map(ConversationResponse::fromEntity));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable Long conversationId,
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(MessageResponse.fromEntity(
                chatService.sendMessage(conversationId, request.getContent(), user)));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<Page<MessageResponse>> getMessages(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(chatService.getMessages(conversationId, user, page, size)
                .map(MessageResponse::fromEntity));
    }

    @PutMapping("/conversations/{conversationId}/read")
    public ResponseEntity<Map<String, String>> markAsRead(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal User user) {
        chatService.markAsRead(conversationId, user);
        return ResponseEntity.ok(Map.of("message", "Marked as read"));
    }
}
