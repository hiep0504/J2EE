package com.example.Backend_J2EE.service;

import com.example.Backend_J2EE.dto.chat.ChatMessageResponse;
import com.example.Backend_J2EE.dto.chat.ConversationDetailsResponse;
import com.example.Backend_J2EE.dto.chat.ConversationSummaryResponse;
import com.example.Backend_J2EE.dto.chat.UserChatHistoryResponse;
import com.example.Backend_J2EE.entity.Account;
import com.example.Backend_J2EE.entity.ChatMessage;
import com.example.Backend_J2EE.entity.Conversation;
import com.example.Backend_J2EE.repository.AccountRepository;
import com.example.Backend_J2EE.repository.ChatMessageRepository;
import com.example.Backend_J2EE.repository.ConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private ConversationRepository conversationRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private ChatPresenceService chatPresenceService;
    @Mock private SimpMessagingTemplate messagingTemplate;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(conversationRepository, chatMessageRepository, accountRepository, chatPresenceService, messagingTemplate, 60, 20, "chief");
    }

    @Test
    void startConversationCreatesConversationAndMessage() {
        Account user = Account.builder().id(1).username("user1").role(Account.Role.user).locked(false).build();
        Conversation conversation = Conversation.builder().id(10).user(user).status(Conversation.Status.OPEN).build();
        ChatMessage message = ChatMessage.builder()
                .id(100)
                .conversation(conversation)
                .sender(user)
                .senderType(ChatMessage.SenderType.USER)
                .content("Hello")
                .createdAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .build();

        when(accountRepository.findById(1)).thenReturn(Optional.of(user));
        when(conversationRepository.findTopByUser_IdAndStatusInOrderByLastMessageAtDesc(1, List.of(Conversation.Status.OPEN, Conversation.Status.ASSIGNED)))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation value = invocation.getArgument(0);
            value.setId(10);
            return value;
        });
        when(conversationRepository.findById(10)).thenReturn(Optional.of(conversation));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(message);
        when(chatMessageRepository.findByConversation_IdOrderByCreatedAtAsc(10)).thenReturn(List.of(message));

        ConversationDetailsResponse response = chatService.startConversation(1, " Hello ");

        assertEquals(10, response.getConversation().getId());
        assertEquals(1, response.getMessages().size());
        assertEquals("Hello", response.getMessages().get(0).getContent());
        verify(messagingTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq("/topic/admin/conversations"), org.mockito.ArgumentMatchers.<Object>any());
    }

    @Test
    void sendMessageByUserRejectsForeignConversation() {
        Account user = Account.builder().id(1).username("user1").role(Account.Role.user).locked(false).build();
        Account otherUser = Account.builder().id(2).username("user2").build();
        Conversation conversation = Conversation.builder().id(10).user(otherUser).status(Conversation.Status.OPEN).build();

        when(accountRepository.findById(1)).thenReturn(Optional.of(user));
        when(conversationRepository.findByIdForUpdate(10)).thenReturn(Optional.of(conversation));

        assertThrows(ResponseStatusException.class, () -> chatService.sendMessageByUser(1, 10, "hello"));
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    void pickConversationAssignsAdmin() {
        Account admin = Account.builder().id(9).username("admin").role(Account.Role.admin).locked(false).build();
        Account user = Account.builder().id(1).username("user1").role(Account.Role.user).build();
        Conversation conversation = Conversation.builder().id(20).user(user).status(Conversation.Status.OPEN).build();

        when(accountRepository.findById(9)).thenReturn(Optional.of(admin));
        when(conversationRepository.findByIdForUpdate(20)).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConversationSummaryResponse response = chatService.pickConversation(9, 20);

        assertEquals(20, response.getId());
        assertEquals(9, response.getAdminId());
    }

    @Test
    void pickConversationRejectsWhenClosed() {
        Account admin = Account.builder().id(9).username("admin").role(Account.Role.admin).locked(false).build();
        Conversation conversation = Conversation.builder().id(20).status(Conversation.Status.CLOSED).build();

        when(accountRepository.findById(9)).thenReturn(Optional.of(admin));
        when(conversationRepository.findByIdForUpdate(20)).thenReturn(Optional.of(conversation));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> chatService.pickConversation(9, 20));

        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void pickConversationRejectsWhenPickedByOtherAdmin() {
        Account admin = Account.builder().id(9).username("admin").role(Account.Role.admin).locked(false).build();
        Account otherAdmin = Account.builder().id(10).username("other").role(Account.Role.admin).locked(false).build();
        Conversation conversation = Conversation.builder().id(20).status(Conversation.Status.ASSIGNED).admin(otherAdmin).build();

        when(accountRepository.findById(9)).thenReturn(Optional.of(admin));
        when(conversationRepository.findByIdForUpdate(20)).thenReturn(Optional.of(conversation));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> chatService.pickConversation(9, 20));

        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void takeoverConversationRejectsWhenCurrentAdminOnlineAndNotSuperAdmin() {
        Account admin = Account.builder().id(9).username("admin").role(Account.Role.admin).locked(false).build();
        Account current = Account.builder().id(10).username("other").role(Account.Role.admin).locked(false).build();
        Conversation conversation = Conversation.builder().id(21).status(Conversation.Status.ASSIGNED).admin(current).build();

        when(accountRepository.findById(9)).thenReturn(Optional.of(admin));
        when(conversationRepository.findByIdForUpdate(21)).thenReturn(Optional.of(conversation));
        when(chatPresenceService.isAdminOnline(10)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> chatService.takeoverConversation(9, 21));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void takeoverConversationAllowsSuperAdminEvenWhenCurrentAdminOnline() {
        chatService = new ChatService(conversationRepository, chatMessageRepository, accountRepository, chatPresenceService, messagingTemplate, 60, 20, "chief");
        Account superAdmin = Account.builder().id(99).username("chief").role(Account.Role.admin).locked(false).build();
        Account current = Account.builder().id(10).username("other").role(Account.Role.admin).locked(false).build();
        Conversation conversation = Conversation.builder().id(22).status(Conversation.Status.ASSIGNED).admin(current).build();

        when(accountRepository.findById(99)).thenReturn(Optional.of(superAdmin));
        when(conversationRepository.findByIdForUpdate(22)).thenReturn(Optional.of(conversation));
        when(chatPresenceService.isAdminOnline(10)).thenReturn(true);
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConversationSummaryResponse response = chatService.takeoverConversation(99, 22);

        assertEquals(22, response.getId());
        assertEquals(99, response.getAdminId());
    }

    @Test
    void closeConversationRejectsWhenNotAssignedAdminAndNotSuperAdmin() {
        Account admin = Account.builder().id(9).username("admin").role(Account.Role.admin).locked(false).build();
        Account ownerAdmin = Account.builder().id(10).username("owner").role(Account.Role.admin).locked(false).build();
        Conversation conversation = Conversation.builder().id(23).status(Conversation.Status.ASSIGNED).admin(ownerAdmin).build();

        when(accountRepository.findById(9)).thenReturn(Optional.of(admin));
        when(conversationRepository.findByIdForUpdate(23)).thenReturn(Optional.of(conversation));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> chatService.closeConversation(9, 23));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void sendMessageByAdminRejectsWhenConversationNotAssignedToAdmin() {
        Account admin = Account.builder().id(9).username("admin").role(Account.Role.admin).locked(false).build();
        Account otherAdmin = Account.builder().id(10).username("other").role(Account.Role.admin).locked(false).build();
        Conversation conversation = Conversation.builder().id(24).status(Conversation.Status.ASSIGNED).admin(otherAdmin).build();

        when(accountRepository.findById(9)).thenReturn(Optional.of(admin));
        when(conversationRepository.findByIdForUpdate(24)).thenReturn(Optional.of(conversation));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> chatService.sendMessageByAdmin(9, 24, "reply"));

        assertEquals(403, ex.getStatusCode().value());
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }

    @Test
    void sendMessageByUserRejectsWhenConversationClosed() {
        Account user = Account.builder().id(1).username("user1").role(Account.Role.user).locked(false).build();
        Conversation conversation = Conversation.builder()
                .id(10)
                .user(user)
                .status(Conversation.Status.CLOSED)
                .build();

        when(accountRepository.findById(1)).thenReturn(Optional.of(user));
        when(conversationRepository.findByIdForUpdate(10)).thenReturn(Optional.of(conversation));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> chatService.sendMessageByUser(1, 10, "hello"));

        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void releaseTimedOutConversationsUnassignsAndBroadcasts() {
        Account user = Account.builder().id(1).username("user1").build();
        Account admin = Account.builder().id(2).username("admin1").role(Account.Role.admin).build();
        Conversation conversation = Conversation.builder().id(30).user(user).admin(admin).status(Conversation.Status.ASSIGNED).build();

        when(conversationRepository.findTimedOutConversations(
            org.mockito.ArgumentMatchers.eq(Conversation.Status.ASSIGNED),
            org.mockito.ArgumentMatchers.eq(Conversation.SenderType.USER),
            org.mockito.ArgumentMatchers.any()
        )).thenReturn(List.of());

        chatService.releaseTimedOutConversations();

        verify(conversationRepository, never()).saveAll(any());
    }

    @Test
    void releaseTimedOutConversationsReopensAssignedConversations() {
        Account user = Account.builder().id(1).username("user1").build();
        Account admin = Account.builder().id(2).username("admin1").role(Account.Role.admin).build();
        Conversation timedOut = Conversation.builder().id(31).user(user).admin(admin).status(Conversation.Status.ASSIGNED).build();

        when(conversationRepository.findTimedOutConversations(
                eq(Conversation.Status.ASSIGNED),
                eq(Conversation.SenderType.USER),
                any()
        )).thenReturn(List.of(timedOut));

        chatService.releaseTimedOutConversations();

        assertNull(timedOut.getAdmin());
        assertEquals(Conversation.Status.OPEN, timedOut.getStatus());
        verify(conversationRepository).saveAll(List.of(timedOut));
    }

    @Test
    void getMyConversationHistoryMapsConversations() {
        Account user = Account.builder().id(1).username("user1").build();
        Conversation conversation = Conversation.builder().id(40).user(user).status(Conversation.Status.OPEN).build();
        ChatMessage message = ChatMessage.builder().id(200).conversation(conversation).sender(user).senderType(ChatMessage.SenderType.USER).content("Hi").createdAt(LocalDateTime.now()).build();

        when(accountRepository.findById(1)).thenReturn(Optional.of(user));
        when(conversationRepository.findByUser_IdOrderByLastMessageAtDesc(1)).thenReturn(List.of(conversation));
        when(chatMessageRepository.findByConversation_IdOrderByCreatedAtAsc(40)).thenReturn(List.of(message));

        UserChatHistoryResponse response = chatService.getMyConversationHistory(1);

        assertEquals(1, response.getConversations().size());
        assertEquals(40, response.getConversations().get(0).getConversation().getId());
        assertEquals("Hi", response.getConversations().get(0).getMessages().get(0).getContent());
    }

    @Test
    void getMyActiveConversationReturnsEmptyWhenNoConversation() {
        Account user = Account.builder().id(1).username("user1").role(Account.Role.user).locked(false).build();
        when(accountRepository.findById(1)).thenReturn(Optional.of(user));
        when(conversationRepository.findTopByUser_IdAndStatusInOrderByLastMessageAtDesc(1, List.of(Conversation.Status.OPEN, Conversation.Status.ASSIGNED)))
                .thenReturn(Optional.empty());
        when(conversationRepository.findTopByUser_IdOrderByLastMessageAtDesc(1)).thenReturn(Optional.empty());

        ConversationDetailsResponse response = chatService.getMyActiveConversation(1);

        assertNull(response.getConversation());
        assertTrue(response.getMessages().isEmpty());
    }

    @Test
    void getConversationDetailsForAdminRejectsOtherAdminWithoutSuperRole() {
        Account admin = Account.builder().id(9).username("admin").role(Account.Role.admin).locked(false).build();
        Account assigned = Account.builder().id(10).username("assigned").role(Account.Role.admin).locked(false).build();
        Account user = Account.builder().id(1).username("user").role(Account.Role.user).locked(false).build();
        Conversation conversation = Conversation.builder().id(50).admin(assigned).user(user).status(Conversation.Status.ASSIGNED).build();

        when(accountRepository.findById(9)).thenReturn(Optional.of(admin));
        when(conversationRepository.findById(50)).thenReturn(Optional.of(conversation));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> chatService.getConversationDetailsForAdmin(9, 50));

        assertEquals(403, ex.getStatusCode().value());
    }
}