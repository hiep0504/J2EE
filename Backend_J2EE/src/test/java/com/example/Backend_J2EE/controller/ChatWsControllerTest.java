package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.config.ChatPrincipal;
import com.example.Backend_J2EE.dto.chat.ChatWsSendRequest;
import com.example.Backend_J2EE.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatWsControllerTest {

    @Mock
    private ChatService chatService;

    private ChatWsController controller;

    @BeforeEach
    void setUp() {
        controller = new ChatWsController(chatService);
    }

    @Test
    void sendsAsAdminWhenPrincipalIsAdmin() {
        ChatWsSendRequest request = new ChatWsSendRequest();
        request.setConversationId(5);
        request.setContent("hi");

        controller.sendMessage(request, new ChatPrincipal(1, "admin", "admin"));

        verify(chatService).sendMessageByAdmin(1, 5, "hi");
    }

    @Test
    void sendsAsUserWhenPrincipalIsUser() {
        ChatWsSendRequest request = new ChatWsSendRequest();
        request.setConversationId(7);
        request.setContent("hello");

        controller.sendMessage(request, new ChatPrincipal(12, "user", "user"));

        verify(chatService).sendMessageByUser(12, 7, "hello");
    }

    @Test
    void ignoresInvalidPrincipalAndNullInputs() {
        controller.sendMessage(new ChatWsSendRequest(), new Principal() {
            @Override
            public String getName() {
                return "x";
            }
        });

        controller.sendMessage(null, new ChatPrincipal(12, "user", "user"));

        verifyNoInteractions(chatService);
        verify(chatService, never()).sendMessageByAdmin(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any());
    }
}