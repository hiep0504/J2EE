package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.config.ChatPrincipal;
import com.example.Backend_J2EE.dto.chat.ChatWsSendRequest;
import com.example.Backend_J2EE.service.ChatService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatWsController {

    private final ChatService chatService;

    public ChatWsController(ChatService chatService) {
        this.chatService = chatService;
    }

    @MessageMapping("/chat.send")
    public void sendMessage(ChatWsSendRequest request, Principal principal) {
        if (!(principal instanceof ChatPrincipal chatPrincipal) || request == null || request.getConversationId() == null) {
            return;
        }

        if (chatPrincipal.isAdmin()) {
            chatService.sendMessageByAdmin(chatPrincipal.getAccountId(), request.getConversationId(), request.getContent());
            return;
        }

        chatService.sendMessageByUser(chatPrincipal.getAccountId(), request.getConversationId(), request.getContent());
    }
}
