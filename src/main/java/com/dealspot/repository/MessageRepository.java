package com.dealspot.repository;

import com.dealspot.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MessageRepository extends JpaRepository<Message, Long> {
    Page<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId, Pageable pageable);

    @Modifying
    @Query("UPDATE Message m SET m.read = true WHERE m.conversation.id = :conversationId AND m.sender.id != :userId AND m.read = false")
    void markAsRead(Long conversationId, Long userId);

    long countByConversationIdAndSenderIdNotAndReadFalse(Long conversationId, Long userId);
}
