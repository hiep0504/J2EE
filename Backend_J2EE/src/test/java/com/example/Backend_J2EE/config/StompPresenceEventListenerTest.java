package com.example.Backend_J2EE.config;

import com.example.Backend_J2EE.service.ChatPresenceService;
import com.example.Backend_J2EE.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.CloseStatus;

import java.security.Principal;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StompPresenceEventListenerTest {

    @Mock
    private ChatPresenceService chatPresenceService;

    @Mock
    private ChatService chatService;

    private StompPresenceEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new StompPresenceEventListener(chatPresenceService, chatService);
    }

    @Test
    void onConnectedMarksAdminOnline() {
        ChatPrincipal principal = new ChatPrincipal(1, "admin", "admin");
        listener.onConnected(new SessionConnectedEvent(this, stompMessage(StompCommand.CONNECT, principal, "s1")));

        verify(chatPresenceService).markAdminOnline(1, "s1");
    }

    @Test
    void onConnectedIgnoresNonAdminOrMissingPrincipal() {
        listener.onConnected(new SessionConnectedEvent(this, stompMessage(StompCommand.CONNECT, new SimplePrincipal("user"), "s2")));

        verify(chatPresenceService, never()).markAdminOnline(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void onDisconnectedMarksAdminOffline() {
        ChatPrincipal principal = new ChatPrincipal(2, "admin2", "admin");
        listener.onDisconnected(new SessionDisconnectEvent(this, stompMessage(StompCommand.DISCONNECT, principal, "s3"), "s3", CloseStatus.NORMAL));

        verify(chatPresenceService).markAdminSessionOffline(2, "s3");
    }

    @Test
    void onDisconnectedIgnoresNonAdminPrincipal() {
        listener.onDisconnected(new SessionDisconnectEvent(this, stompMessage(StompCommand.DISCONNECT, new SimplePrincipal("user"), "s4"), "s4", CloseStatus.NORMAL));

        verify(chatPresenceService, never()).markAdminSessionOffline(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void onDisconnectedIgnoresMissingUser() {
        listener.onDisconnected(new SessionDisconnectEvent(this, stompMessage(StompCommand.DISCONNECT, null, "s5"), "s5", CloseStatus.NORMAL));

        verify(chatPresenceService, never()).markAdminSessionOffline(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private Message<byte[]> stompMessage(StompCommand command, Principal principal, String sessionId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId(sessionId);
        if (principal != null) {
            accessor.setUser(principal);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private static class SimplePrincipal implements Principal {
        private final String name;

        private SimplePrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}