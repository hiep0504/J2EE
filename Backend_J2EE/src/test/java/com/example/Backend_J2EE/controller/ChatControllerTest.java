package com.example.Backend_J2EE.controller;

import com.example.Backend_J2EE.dto.chat.ChatMessageResponse;
import com.example.Backend_J2EE.dto.chat.ConversationDetailsResponse;
import com.example.Backend_J2EE.dto.chat.ConversationSummaryResponse;
import com.example.Backend_J2EE.dto.chat.SendMessageRequest;
import com.example.Backend_J2EE.dto.chat.StartConversationRequest;
import com.example.Backend_J2EE.dto.chat.UserChatHistoryResponse;
import com.example.Backend_J2EE.entity.Account;
import com.example.Backend_J2EE.entity.Conversation;
import com.example.Backend_J2EE.service.AdminAuthorizationService;
import com.example.Backend_J2EE.service.AuthService;
import com.example.Backend_J2EE.service.ChatService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @Mock
    private AdminAuthorizationService adminAuthorizationService;

    @Mock
    private HttpSession session;

    private ChatController controller;

    @BeforeEach
    void setUp() {
        controller = new ChatController(chatService, adminAuthorizationService);
    }

    @Test
    void startConversationDelegatesUsingSessionAccountId() {
        when(session.getAttribute(AuthService.SESSION_ACCOUNT_ID)).thenReturn(1);
        ConversationSummaryResponse summary = new ConversationSummaryResponse(10, 1, "user", null, null, Conversation.Status.OPEN, null, null);
        ConversationDetailsResponse response = new ConversationDetailsResponse(summary, List.of());
        when(chatService.startConversation(1, "hello")).thenReturn(response);

        ConversationDetailsResponse actual = controller.startConversation(new StartConversationRequest() {{ setContent("hello"); }}, session);

        assertEquals(10, actual.getConversation().getId());
    }

    @Test
    void getOpenConversationsRequiresAdmin() {
        when(adminAuthorizationService.requireAdmin(session)).thenReturn(Account.builder().id(9).username("admin").role(Account.Role.admin).build());
        when(chatService.getOpenConversations()).thenReturn(List.of());

        assertEquals(0, controller.getOpenConversations(session).size());
        verify(adminAuthorizationService).requireAdmin(session);
    }

    @Test
    void sendMessageAsUserDelegates() {
        when(session.getAttribute(AuthService.SESSION_ACCOUNT_ID)).thenReturn(1);
        ChatMessageResponse messageResponse = new ChatMessageResponse();
        when(chatService.sendMessageByUser(1, 20, "hi")).thenReturn(messageResponse);

        ChatMessageResponse response = controller.sendMessageAsUser(20, new SendMessageRequest() {{ setContent("hi"); }}, session);

        assertSame(messageResponse, response);
    }

    @Test
    void userAndAdminEndpointsDelegate() {
        when(session.getAttribute(AuthService.SESSION_ACCOUNT_ID)).thenReturn(2);
        Account admin = Account.builder().id(4).username("admin").role(Account.Role.admin).build();
        when(adminAuthorizationService.requireAdmin(session)).thenReturn(admin);

        ConversationSummaryResponse summary = new ConversationSummaryResponse(10, 2, "user", 4, "admin", Conversation.Status.ASSIGNED, null, null);
        ConversationDetailsResponse details = new ConversationDetailsResponse(summary, List.of());
        UserChatHistoryResponse history = new UserChatHistoryResponse(List.of(details));
        ChatMessageResponse message = new ChatMessageResponse();

        when(chatService.getMyActiveConversation(2)).thenReturn(details);
        when(chatService.getMyConversationHistory(2)).thenReturn(history);
        when(chatService.getConversationDetailsForUser(2, 3)).thenReturn(details);
        when(chatService.getAssignedConversations()).thenReturn(List.of(summary));
        when(chatService.getMyAssignedConversations(4)).thenReturn(List.of(summary));
        when(chatService.getConversationDetailsForAdmin(4, 5)).thenReturn(details);
        when(chatService.pickConversation(4, 5)).thenReturn(summary);
        when(chatService.takeoverConversation(4, 5)).thenReturn(summary);
        when(chatService.sendMessageByAdmin(4, 5, "hi")).thenReturn(message);
        when(chatService.closeConversation(4, 5)).thenReturn(summary);

        assertSame(details, controller.getMyActiveConversation(session));
        assertSame(history, controller.getMyConversationHistory(session));
        assertSame(details, controller.getConversationForUser(3, session));
        assertEquals(1, controller.getAssignedConversations(session).size());
        assertEquals(1, controller.getMyAssignedConversations(session).size());
        assertSame(details, controller.getConversationForAdmin(5, session));
        assertSame(summary, controller.pickConversation(5, session));
        assertSame(summary, controller.takeoverConversation(5, session));
        assertSame(message, controller.sendMessageAsAdmin(5, new SendMessageRequest() {{ setContent("hi"); }}, session));
        assertSame(summary, controller.closeConversation(5, session));

        verify(adminAuthorizationService, times(7)).requireAdmin(session);
        verify(chatService).getMyActiveConversation(2);
        verify(chatService).getMyConversationHistory(2);
        verify(chatService).getConversationDetailsForUser(2, 3);
        verify(chatService).getAssignedConversations();
        verify(chatService).getMyAssignedConversations(4);
        verify(chatService).getConversationDetailsForAdmin(4, 5);
        verify(chatService).pickConversation(4, 5);
        verify(chatService).takeoverConversation(4, 5);
        verify(chatService).sendMessageByAdmin(4, 5, "hi");
        verify(chatService).closeConversation(4, 5);
    }

    @Test
    void nullRequestContentPassesThrough() {
        when(session.getAttribute(AuthService.SESSION_ACCOUNT_ID)).thenReturn(1);
        when(chatService.startConversation(1, null)).thenReturn(new ConversationDetailsResponse());
        when(chatService.sendMessageByUser(1, 20, null)).thenReturn(new ChatMessageResponse());

        controller.startConversation(null, session);
        controller.sendMessageAsUser(20, null, session);

        verify(chatService).startConversation(1, null);
        verify(chatService).sendMessageByUser(1, 20, null);
    }
}
