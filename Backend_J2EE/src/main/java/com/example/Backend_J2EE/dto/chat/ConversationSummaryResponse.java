package com.example.Backend_J2EE.dto.chat;

import com.example.Backend_J2EE.entity.Conversation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSummaryResponse {

    private Integer id;
    private Integer userId;
    private String username;
    private Integer adminId;
    private String adminUsername;
    private Conversation.Status status;
    private LocalDateTime lastMessageAt;
    private LocalDateTime assignedAt;
}
