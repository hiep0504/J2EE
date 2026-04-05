package com.example.Backend_J2EE.config;

import com.example.Backend_J2EE.service.ChatPresenceService;
import com.example.Backend_J2EE.service.ChatService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class StompPresenceEventListener {

    private final ChatPresenceService chatPresenceService;
    private final ChatService chatService;

    public StompPresenceEventListener(ChatPresenceService chatPresenceService, ChatService chatService) {
        this.chatPresenceService = chatPresenceService;
        this.chatService = chatService;
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        if (!(accessor.getUser() instanceof ChatPrincipal principal)) {
            return;
        }

        if (principal.isAdmin()) {
            chatPresenceService.markAdminOnline(principal.getAccountId(), sessionId);
        }
    }

    @EventListener
    public void onDisconnected(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        if (!(accessor.getUser() instanceof ChatPrincipal principal)) {
            return;
        }

        if (!principal.isAdmin()) {
            return;
        }

        chatPresenceService.markAdminSessionOffline(principal.getAccountId(), sessionId);
    }
}
