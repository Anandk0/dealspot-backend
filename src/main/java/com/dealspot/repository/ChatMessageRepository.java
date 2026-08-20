package com.dealspot.repository;

import com.dealspot.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversationIdOrderByCreatedAt(Long conversationId);

    long countByConversationIdAndReadFalseAndSenderIdNot(Long conversationId, Long senderId);
}
