package com.example.Backend_J2EE.config;

import com.example.Backend_J2EE.entity.Account;
import com.example.Backend_J2EE.repository.AccountRepository;
import com.example.Backend_J2EE.service.AuthService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final AccountRepository accountRepository;

    public StompAuthChannelInterceptor(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs == null) {
            return message;
        }

        Object accountIdObj = attrs.get(AuthService.SESSION_ACCOUNT_ID);
        if (!(accountIdObj instanceof Integer accountId)) {
            return message;
        }

        Account account = accountRepository.findById(accountId).orElse(null);
        if (account == null) {
            return message;
        }

        String role = account.getRole() != null ? account.getRole().name() : "user";
        accessor.setUser(new ChatPrincipal(account.getId(), account.getUsername(), role));
        return message;
    }
}
