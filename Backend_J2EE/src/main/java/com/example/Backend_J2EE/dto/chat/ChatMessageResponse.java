package com.example.Backend_J2EE.dto.chat;

import com.example.Backend_J2EE.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {

    private Integer id;
    private Integer conversationId;
    private Integer senderId;
    private ChatMessage.SenderType senderType;
    private String senderName;
    private String content;
    private LocalDateTime createdAt;
}
