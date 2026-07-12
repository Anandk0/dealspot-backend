package com.dealspot.controller;

import com.dealspot.entity.Conversation;
import com.dealspot.entity.Message;
import com.dealspot.entity.User;
import com.dealspot.service.ChatService;
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
    public ResponseEntity<Conversation> startConversation(
            @PathVariable Long listingId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.getOrCreateConversation(listingId, user));
    }

    @GetMapping("/conversations")
    public ResponseEntity<Page<Conversation>> getConversations(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(chatService.getMyConversations(user.getId(), page, size));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<Message> sendMessage(
            @PathVariable Long conversationId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.sendMessage(conversationId, body.get("content"), user));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<Page<Message>> getMessages(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(chatService.getMessages(conversationId, user, page, size));
    }

    @PutMapping("/conversations/{conversationId}/read")
    public ResponseEntity<Map<String, String>> markAsRead(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal User user) {
        chatService.markAsRead(conversationId, user);
        return ResponseEntity.ok(Map.of("message", "Marked as read"));
    }
}
