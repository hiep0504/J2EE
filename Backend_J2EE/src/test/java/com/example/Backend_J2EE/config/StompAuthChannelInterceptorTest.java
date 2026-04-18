package com.example.Backend_J2EE.config;

import com.example.Backend_J2EE.entity.Account;
import com.example.Backend_J2EE.repository.AccountRepository;
import com.example.Backend_J2EE.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StompAuthChannelInterceptorTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private StompAuthChannelInterceptor interceptor;

    @Test
    void preSendReturnsMessageWhenAccessorIsMissing() {
        Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();

        Message<?> result = interceptor.preSend(message, null);

        assertSame(message, result);
        verifyNoRepoInteractions();
    }

    @Test
    void preSendReturnsMessageWhenCommandIsNotConnect() {
        Message<byte[]> message = stompMessage(StompCommand.SEND, Map.of(AuthService.SESSION_ACCOUNT_ID, 1));

        Message<?> result = interceptor.preSend(message, null);

        assertSame(message, result);
        verifyNoRepoInteractions();
    }

    @Test
    void preSendReturnsMessageWhenSessionAttributesAreMissing() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, null);

        assertSame(message, result);
        verifyNoRepoInteractions();
    }

    @Test
    void preSendReturnsMessageWhenSessionAccountIdIsInvalid() {
        Message<byte[]> message = stompMessage(StompCommand.CONNECT, Map.of(AuthService.SESSION_ACCOUNT_ID, "bad"));

        Message<?> result = interceptor.preSend(message, null);

        assertSame(message, result);
        verifyNoRepoInteractions();
    }

    @Test
    void preSendReturnsMessageWhenAccountNotFound() {
        Message<byte[]> message = stompMessage(StompCommand.CONNECT, Map.of(AuthService.SESSION_ACCOUNT_ID, 10));
        when(accountRepository.findById(10)).thenReturn(Optional.empty());

        Message<?> result = interceptor.preSend(message, null);

        assertSame(message, result);
        verify(accountRepository).findById(10);
    }

    @Test
    void preSendAttachesChatPrincipalForAdminAccount() {
        Account account = Account.builder().id(11).username("adminUser").role(Account.Role.admin).build();
        Message<byte[]> message = stompMessage(StompCommand.CONNECT, Map.of(AuthService.SESSION_ACCOUNT_ID, 11));
        when(accountRepository.findById(11)).thenReturn(Optional.of(account));

        Message<?> result = interceptor.preSend(message, null);
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);

        assertTrue(accessor.getUser() instanceof ChatPrincipal);
        ChatPrincipal principal = (ChatPrincipal) accessor.getUser();
        assertEquals(account.getId(), principal.getAccountId());
        assertTrue(principal.isAdmin());
    }

    @Test
    void preSendDefaultsRoleToUserWhenAccountRoleIsNull() {
        Account account = Account.builder().id(12).username("plainUser").role(null).build();
        Message<byte[]> message = stompMessage(StompCommand.CONNECT, Map.of(AuthService.SESSION_ACCOUNT_ID, 12));
        when(accountRepository.findById(12)).thenReturn(Optional.of(account));

        Message<?> result = interceptor.preSend(message, null);
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);

        assertInstanceOf(ChatPrincipal.class, accessor.getUser());
        ChatPrincipal principal = (ChatPrincipal) accessor.getUser();
        assertTrue(!principal.isAdmin());
    }

    private Message<byte[]> stompMessage(StompCommand command, Map<String, Object> sessionAttributes) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        Map<String, Object> attrs = sessionAttributes == null ? new HashMap<>() : new HashMap<>(sessionAttributes);
        accessor.setSessionAttributes(attrs);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private void verifyNoRepoInteractions() {
        verify(accountRepository, never()).findById(org.mockito.ArgumentMatchers.any());
    }
}