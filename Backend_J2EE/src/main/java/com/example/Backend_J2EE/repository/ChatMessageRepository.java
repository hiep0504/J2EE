package com.example.Backend_J2EE.repository;

import com.example.Backend_J2EE.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {

    List<ChatMessage> findByConversation_IdOrderByCreatedAtAsc(Integer conversationId);
}
